package com.example.bankcards.util;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class CardExpiryUtilTest {

    private final CardExpiryUtil util = new CardExpiryUtil();

    @Test
    void isExpired_NullMonthOrYear_ReturnsFalse() {
        assertFalse(util.isExpired(null, 2030));
        assertFalse(util.isExpired(12, null));
        assertFalse(util.isExpired(null, null));
    }

    @Test
    void isExpired_CurrentMonth_ReturnsFalse() {
        YearMonth now = YearMonth.now();
        assertFalse(util.isExpired(now.getMonthValue(), now.getYear()));
    }

    @Test
    void isExpired_PastMonth_ReturnsTrue() {
        YearMonth past = YearMonth.now().minusMonths(1);
        assertTrue(util.isExpired(past.getMonthValue(), past.getYear()));
    }

    @Test
    void isExpired_FutureMonth_ReturnsFalse() {
        YearMonth future = YearMonth.now().plusMonths(1);
        assertFalse(util.isExpired(future.getMonthValue(), future.getYear()));
    }
}
