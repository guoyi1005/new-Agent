package com.example.appbackend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobHotScoreServiceTest {
    @Test void scoreIsStableAndBounded() {
        var scorer = new JobHotScoreService();
        var first = scorer.score(100, 20, BigDecimal.valueOf(18000), .5, .7);
        assertEquals(first, scorer.score(100, 20, BigDecimal.valueOf(18000), .5, .7));
        assertTrue(first.signum() >= 0 && first.compareTo(BigDecimal.valueOf(100)) <= 0);
    }
}
