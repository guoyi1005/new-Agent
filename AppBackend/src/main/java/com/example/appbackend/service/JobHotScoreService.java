package com.example.appbackend.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class JobHotScoreService {
    public BigDecimal score(long active30d, long new7d, BigDecimal salaryP50,
                            double largeCompanyRatio, double campusFriendlyRatio) {
        double demand = active30d <= 0 ? 0 : Math.min(100, 100 * Math.log1p(active30d) / Math.log1p(1000));
        double growth = active30d <= 0 ? 0 : Math.min(100, (double) new7d / active30d * 500);
        double salary = salaryP50 == null ? 0 : Math.min(100, salaryP50.doubleValue() / 300);
        double company = Math.max(0, Math.min(100, largeCompanyRatio * 100));
        double campus = Math.max(0, Math.min(100, campusFriendlyRatio * 100));
        return BigDecimal.valueOf(0.35 * demand + 0.20 * growth + 0.20 * salary + 0.15 * company + 0.10 * campus)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
