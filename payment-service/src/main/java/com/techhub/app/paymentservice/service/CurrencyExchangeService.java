package com.techhub.app.paymentservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CurrencyExchangeService {

    private static final String API_URL = "https://open.er-api.com/v6/latest/";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final Map<String, CachedRates> cache = new ConcurrentHashMap<>();

    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        String f = normalize(from);
        String t = normalize(to);
        if (f.equals(t)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rate = getRate(f, t);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRate(String from, String to) {
        String f = normalize(from);
        String t = normalize(to);
        if (f.equals(t)) {
            return BigDecimal.ONE;
        }
        CachedRates cached = cache.get(f);
        if (cached == null || cached.isExpired()) {
            cached = fetchRates(f);
            if (cached != null) {
                cache.put(f, cached);
            }
        }
        if (cached == null) {
            throw new IllegalStateException("Unable to fetch exchange rates for base " + f);
        }
        BigDecimal rate = cached.rates.get(t);
        if (rate == null) {
            throw new IllegalArgumentException("Unsupported currency: " + t);
        }
        return rate;
    }

    private CachedRates fetchRates(String base) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + base))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("FX api returned status {} for base {}", resp.statusCode(), base);
                return null;
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode rates = root.path("rates");
            if (rates.isMissingNode()) {
                log.warn("FX api response missing 'rates' for base {}", base);
                return null;
            }
            Map<String, BigDecimal> map = new ConcurrentHashMap<>();
            rates.fields().forEachRemaining(e -> map.put(e.getKey(), new BigDecimal(e.getValue().asText())));
            return new CachedRates(map, Instant.now().plus(CACHE_TTL));
        } catch (Exception ex) {
            log.error("Failed to fetch FX rates for base {}: {}", base, ex.getMessage());
            return null;
        }
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            return "VND";
        }
        return code.trim().toUpperCase();
    }

    private static class CachedRates {
        final Map<String, BigDecimal> rates;
        final Instant expiresAt;

        CachedRates(Map<String, BigDecimal> rates, Instant expiresAt) {
            this.rates = rates;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
