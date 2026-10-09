package com.example.smartwater.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Statistical anomaly rule: latest usage > mean + k x standard deviation (population std-dev) of the
 * household's own recent history. This only flags POTENTIAL abnormal usage - it never proves a leak.
 */
public final class AnomalyDetector {
    private AnomalyDetector() {}

    public record Result(boolean sufficientData, double mean, double stdDev, double threshold, boolean anomalous) {}

    public static Result analyze(List<BigDecimal> history, BigDecimal latest, double multiplier, int minPoints) {
        if (history == null || history.size() < Math.max(2, minPoints) || latest == null) {
            return new Result(false, 0, 0, 0, false);
        }
        double mean = history.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = history.stream().mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2)).sum() / history.size();
        double std = Math.sqrt(variance);
        double threshold = mean + multiplier * std;
        return new Result(true, mean, std, threshold, latest.doubleValue() > threshold);
    }
}
