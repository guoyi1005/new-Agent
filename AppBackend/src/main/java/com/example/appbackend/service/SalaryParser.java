package com.example.appbackend.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SalaryParser {
    private static final Pattern RANGE = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*[-~至到]\\s*(\\d+(?:\\.\\d+)?)\\s*(K|千|元/天|元/日|万(?:/年|\\s*年)?)?(?:[^0-9]{0,8}(\\d+)\\s*薪)?");

    public ParsedSalary parse(String text) {
        if (text == null || text.isBlank() || text.contains("面议")) return new ParsedSalary(null, null, null, null);
        Matcher matcher = RANGE.matcher(text.replace(",", ""));
        if (!matcher.find()) return new ParsedSalary(null, null, null, null);
        BigDecimal multiplier;
        String unit = matcher.group(3);
        if (unit == null || unit.isBlank()) multiplier = BigDecimal.ONE;
        else if (unit.equalsIgnoreCase("K") || unit.equals("千")) multiplier = BigDecimal.valueOf(1000);
        else if (unit.contains("天") || unit.contains("日")) multiplier = BigDecimal.ONE;
        else multiplier = BigDecimal.valueOf(10000);
        String normalizedUnit = unit != null && (unit.contains("天") || unit.contains("日")) ? "day"
                : unit != null && unit.contains("年") ? "year" : "month";
        return new ParsedSalary(new BigDecimal(matcher.group(1)).multiply(multiplier),
                new BigDecimal(matcher.group(2)).multiply(multiplier), normalizedUnit,
                matcher.group(4) == null ? null : Integer.valueOf(matcher.group(4)));
    }

    public record ParsedSalary(BigDecimal min, BigDecimal max, String unit, Integer months) {}
}
