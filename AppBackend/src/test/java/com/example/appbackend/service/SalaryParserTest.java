package com.example.appbackend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SalaryParserTest {
    private final SalaryParser parser = new SalaryParser();

    @Test void parsesMonthlyKAndSalaryMonths() {
        var salary = parser.parse("15-25K·14薪");
        assertEquals(new BigDecimal("15000"), salary.min());
        assertEquals(new BigDecimal("25000"), salary.max());
        assertEquals("month", salary.unit());
        assertEquals(14, salary.months());
    }

    @Test void doesNotInventNegotiablePay() {
        var salary = parser.parse("薪资面议");
        assertNull(salary.min());
        assertNull(salary.max());
    }
}
