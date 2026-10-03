package com.example.appbackend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JobFingerprintTest {
    @Test void equivalentWhitespaceProducesSameFingerprintAndCityMatters() {
        var fingerprint = new JobFingerprint();
        assertEquals(fingerprint.create("某公司", "Java后端开发", "成都", "15-25K"),
                fingerprint.create(" 某公司 ", "Java后端开发", " 成都 ", "15-25K"));
        assertNotEquals(fingerprint.create("某公司", "Java后端开发", "成都", "15-25K"),
                fingerprint.create("某公司", "Java后端开发", "北京", "15-25K"));
    }
}
