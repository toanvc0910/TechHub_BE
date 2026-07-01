"""
SSRF guard for outbound HTTP downloads triggered by user-supplied URLs.

When AI service receives a chat message that references a file payload (which
may contain `url`, `secureUrl`, `publicUrl`, ...), the URL is attacker-influenced
and must not be fetched blindly. Without a guard, an authenticated user could
target `http://169.254.169.254/latest/meta-data` (cloud metadata), internal
admin dashboards, or `file:///etc/passwd`.

This module exposes `validate_outbound_url(url)` which performs:

  1. Scheme allowlist: only `http`, `https` (no `file`, `gopher`, `ftp`, ...).
  2. Host allowlist (if configured via env) — exact match short-circuits the IP
     check, so known internal hosts like `file-service-container` are permitted.
  3. DNS resolution and per-A-record private CIDR rejection (RFC1918,
     loopback, link-local, multicast, reserved). Catches DNS-rebinding /
     compromised-DNS by checking *every* resolved address, not just first.

It is async-safe: DNS resolution runs in a thread so the event loop is not
blocked. Use `await validate_outbound_url(url)` before passing the URL to any
HTTP client.
"""

from __future__ import annotations

import asyncio
import ipaddress
import logging
import os
import socket
from dataclasses import dataclass
from urllib.parse import urlparse

logger = logging.getLogger(__name__)


class UnsafeOutboundUrlError(ValueError):
    """Raised when an outbound URL fails the SSRF guard."""


_ALLOWED_SCHEMES = {"http", "https"}


def _allowed_hosts() -> set[str]:
    """Hostnames whose downloads we trust even when they resolve to a private
    IP (typically the internal file-service hostname in docker / k8s).
    Read fresh each call so tests / `.env` reloads pick changes up.
    """
    raw = os.getenv("AI_FILE_DOWNLOAD_ALLOWED_HOSTS", "").strip()
    extras = {host.strip().lower() for host in raw.split(",") if host.strip()}
    # FILE_SERVICE_BASE_URL host is implicitly trusted: it's our own peer.
    file_service_base = os.getenv("FILE_SERVICE_BASE_URL", "").strip()
    if file_service_base:
        try:
            parsed = urlparse(file_service_base)
            if parsed.hostname:
                extras.add(parsed.hostname.lower())
        except Exception:  # pragma: no cover - defensive
            pass
    return extras


def _is_public_ip(ip_text: str) -> bool:
    try:
        addr = ipaddress.ip_address(ip_text)
    except ValueError:
        return False
    if addr.is_private or addr.is_loopback or addr.is_link_local:
        return False
    if addr.is_multicast or addr.is_reserved or addr.is_unspecified:
        return False
    # 169.254/16 is link_local; covered above. Block 100.64/10 carrier-grade NAT
    # which `ipaddress` does not flag as private.
    if addr.version == 4 and addr in ipaddress.ip_network("100.64.0.0/10"):
        return False
    return True


async def _resolve_addresses(host: str) -> list[str]:
    def resolve() -> list[str]:
        try:
            infos = socket.getaddrinfo(host, None, type=socket.SOCK_STREAM)
        except socket.gaierror:
            return []
        return [item[4][0] for item in infos]

    return await asyncio.to_thread(resolve)


@dataclass(frozen=True, slots=True)
class ValidatedUrl:
    url: str
    host: str
    scheme: str
    addresses: tuple[str, ...]
    trusted_host: bool


async def validate_outbound_url(url: str) -> ValidatedUrl:
    """Raise `UnsafeOutboundUrlError` if `url` should not be fetched.

    Returns a `ValidatedUrl` carrying the parsed host + resolved addresses for
    audit logging if the caller needs them.
    """
    if not isinstance(url, str) or not url.strip():
        raise UnsafeOutboundUrlError("URL is empty.")
    parsed = urlparse(url.strip())
    scheme = (parsed.scheme or "").lower()
    if scheme not in _ALLOWED_SCHEMES:
        raise UnsafeOutboundUrlError(f"Disallowed URL scheme: {scheme!r}")
    host = (parsed.hostname or "").lower()
    if not host:
        raise UnsafeOutboundUrlError("URL has no hostname.")

    allowed_hosts = _allowed_hosts()
    if host in allowed_hosts:
        # Explicitly trusted internal peer — DNS rebinding is moot because we
        # configured the host ourselves.
        return ValidatedUrl(url=url, host=host, scheme=scheme, addresses=(), trusted_host=True)

    # Reject IP-literal hosts that are non-public outright. Note: catch only
    # the ValueError from `ip_address(...)`, never our own UnsafeOutboundUrlError
    # (which subclasses ValueError) — otherwise the raise here would silently
    # fall through to the DNS branch.
    literal: ipaddress.IPv4Address | ipaddress.IPv6Address | None = None
    try:
        literal = ipaddress.ip_address(host)
    except ValueError:
        pass
    if literal is not None:
        if not _is_public_ip(str(literal)):
            raise UnsafeOutboundUrlError(
                f"Refusing to fetch private/loopback/link-local literal IP: {host}"
            )
        return ValidatedUrl(url=url, host=host, scheme=scheme, addresses=(host,), trusted_host=False)

    addresses = await _resolve_addresses(host)
    if not addresses:
        raise UnsafeOutboundUrlError(f"Host did not resolve: {host}")
    for addr in addresses:
        if not _is_public_ip(addr):
            raise UnsafeOutboundUrlError(
                f"Host {host} resolved to non-public address {addr}; "
                "refusing fetch to prevent SSRF."
            )
    return ValidatedUrl(
        url=url,
        host=host,
        scheme=scheme,
        addresses=tuple(addresses),
        trusted_host=False,
    )
