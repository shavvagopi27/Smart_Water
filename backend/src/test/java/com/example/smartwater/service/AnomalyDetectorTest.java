package com.example.smartwater.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnomalyDetectorTest {
    private final List<BigDecimal> history = Arrays.stream("10,12,11,9,10,11,10,12,9,11".split(","))
            .map(BigDecimal::new).toList();

    @Test
    void flagsUsageAboveMeanPlusTwoStdDev() {
        AnomalyDetector.Result r = AnomalyDetector.analyze(history, new BigDecimal("20"), 2.0, 7);
        assertTrue(r.sufficientData());
        assertEquals(10.5, r.mean(), 1e-9);
        assertTrue(r.anomalous());
    }

    @Test
    void normalVariationIsNotFlagged() {
        assertFalse(AnomalyDetector.analyze(history, new BigDecimal("12"), 2.0, 7).anomalous());
    }

    @Test
    void tooLittleHistoryNeverFlags() {
        AnomalyDetector.Result r = AnomalyDetector.analyze(history.subList(0, 3), new BigDecimal("500"), 2.0, 7);
        assertFalse(r.sufficientData());
        assertFalse(r.anomalous());
    }
}
