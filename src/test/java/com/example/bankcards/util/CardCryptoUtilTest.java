package com.example.bankcards.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardCryptoUtilTest {

    private final CardCryptoUtil util = new CardCryptoUtil("test-secret-key");

    @Test
    void encryptDecrypt_RoundTrip() {
        String pan = "1111222233334444";

        String encrypted = util.encrypt(pan);
        assertNotNull(encrypted);
        assertNotEquals(pan, encrypted);

        String decrypted = util.decrypt(encrypted);
        assertEquals(pan, decrypted);
    }

    @Test
    void encrypt_SameInput_ProducesDifferentCiphertext() {
        String pan = "1111222233334444";
        String e1 = util.encrypt(pan);
        String e2 = util.encrypt(pan);
        assertNotEquals(e1, e2);
    }

    @Test
    void decrypt_InvalidInput_Throws() {
        assertThrows(IllegalStateException.class, () -> util.decrypt("not-base64"));
    }

    @Test
    void last4_ValidPan_ReturnsLast4() {
        assertEquals("4444", util.last4("1111222233334444"));
    }

    @Test
    void last4_InvalidPan_Throws() {
        assertThrows(IllegalArgumentException.class, () -> util.last4(null));
        assertThrows(IllegalArgumentException.class, () -> util.last4(""));
        assertThrows(IllegalArgumentException.class, () -> util.last4("123"));
        assertThrows(IllegalArgumentException.class, () -> util.last4("1111 2222 3333"));
        assertThrows(IllegalArgumentException.class, () -> util.last4("111122223333444a"));
    }

    @Test
    void maskLast4_Valid_ReturnsMasked() {
        assertEquals("**** **** **** 1234", util.maskLast4("1234"));
    }

    @Test
    void maskLast4_Invalid_Throws() {
        assertThrows(IllegalArgumentException.class, () -> util.maskLast4(null));
        assertThrows(IllegalArgumentException.class, () -> util.maskLast4(""));
        assertThrows(IllegalArgumentException.class, () -> util.maskLast4("123"));
        assertThrows(IllegalArgumentException.class, () -> util.maskLast4("12345"));
        assertThrows(IllegalArgumentException.class, () -> util.maskLast4("12a4"));
    }
}
