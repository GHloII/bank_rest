package com.example.bankcards.util;

import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class CardExpiryUtil {

    public boolean isExpired(Integer expiryMonth, Integer expiryYear) {
        if (expiryMonth == null || expiryYear == null) {
            return false;
        }
        YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
        YearMonth now = YearMonth.now();
        return expiry.isBefore(now);
    }
}
