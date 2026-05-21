package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.repository.TransactionItemRepository;
import com.techhub.app.paymentservice.repository.projection.CoursePaymentPriceProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoursePaymentPricingService {

    public static final String FX_PROVIDER = "open.er-api.com";

    private final TransactionItemRepository transactionItemRepository;
    private final CurrencyExchangeService currencyExchangeService;

    public PaymentPriceQuote quoteForGateway(UUID courseId, String gatewayCurrency) {
        CoursePaymentPriceProjection course = transactionItemRepository.findCoursePaymentPrice(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found or inactive: " + courseId));

        BigDecimal originalAmount = effectivePrice(course.getPrice(), course.getDiscountPrice());
        if (originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Course price must be greater than 0");
        }

        String originalCurrency = normalizeCurrency(course.getCurrency());
        String targetCurrency = normalizeCurrency(gatewayCurrency);
        OffsetDateTime quotedAt = OffsetDateTime.now();
        BigDecimal fxRate = BigDecimal.ONE.setScale(8, RoundingMode.HALF_UP);
        BigDecimal gatewayAmount = originalAmount;

        if (!originalCurrency.equals(targetCurrency)) {
            fxRate = currencyExchangeService.getRate(originalCurrency, targetCurrency)
                    .setScale(8, RoundingMode.HALF_UP);
            gatewayAmount = originalAmount.multiply(fxRate);
        }

        gatewayAmount = normalizeGatewayAmount(gatewayAmount, targetCurrency);
        if (gatewayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Converted payment amount is too small for " + targetCurrency);
        }

        return PaymentPriceQuote.builder()
                .courseId(course.getCourseId())
                .courseTitle(course.getTitle())
                .originalAmount(originalAmount.setScale(2, RoundingMode.HALF_UP))
                .originalCurrency(originalCurrency)
                .gatewayAmount(gatewayAmount)
                .gatewayCurrency(targetCurrency)
                .fxRate(fxRate)
                .fxProvider(FX_PROVIDER)
                .fxQuotedAt(quotedAt)
                .build();
    }

    private BigDecimal effectivePrice(BigDecimal price, BigDecimal discountPrice) {
        BigDecimal safePrice = price == null ? BigDecimal.ZERO : price;
        if (discountPrice != null
                && discountPrice.compareTo(BigDecimal.ZERO) > 0
                && discountPrice.compareTo(safePrice) < 0) {
            return discountPrice;
        }
        return safePrice;
    }

    private BigDecimal normalizeGatewayAmount(BigDecimal amount, String currency) {
        if ("VND".equals(currency)) {
            return amount.setScale(0, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "VND";
        }
        String normalized = currency.trim().toUpperCase();
        return "USD".equals(normalized) ? "USD" : "VND";
    }
}
