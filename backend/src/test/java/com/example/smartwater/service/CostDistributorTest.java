package com.example.smartwater.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CostDistributorTest {
    private static BigDecimal b(String s) { return new BigDecimal(s); }

    @Test
    void proportionalToConsumption() {
        Map<Long, CostDistributor.Share> r = CostDistributor.distribute(b("20000"), List.of(
                new CostDistributor.Input(1L, b("100"), true, b("1000")),
                new CostDistributor.Input(2L, b("200"), true, b("1000")),
                new CostDistributor.Input(3L, b("300"), true, b("1000")),
                new CostDistributor.Input(4L, b("400"), true, b("1000"))));
        assertEquals(b("2000.00"), r.get(1L).amount());
        assertEquals(b("4000.00"), r.get(2L).amount());
        assertEquals(b("6000.00"), r.get(3L).amount());
        assertEquals(b("8000.00"), r.get(4L).amount());
        assertFalse(r.get(1L).areaFallback());
    }

    @Test
    void unmeteredHouseholdUsesFlatAreaFallback() {
        Map<Long, CostDistributor.Share> r = CostDistributor.distribute(b("3000"), List.of(
                new CostDistributor.Input(1L, b("100"), true, b("1000")),
                new CostDistributor.Input(2L, b("100"), true, b("1000")),
                new CostDistributor.Input(3L, BigDecimal.ZERO, false, b("1000"))));
        // area share of household 3 = 3000 x 1000/3000 = 1000 ; remaining 2000 split 50/50 by consumption
        assertEquals(b("1000.00"), r.get(3L).amount());
        assertTrue(r.get(3L).areaFallback());
        assertEquals(b("1000.00"), r.get(1L).amount());
        assertEquals(b("1000.00"), r.get(2L).amount());
    }

    @Test
    void everybodyUnmeteredSplitsByArea() {
        Map<Long, CostDistributor.Share> r = CostDistributor.distribute(b("1000"), List.of(
                new CostDistributor.Input(1L, BigDecimal.ZERO, false, b("500")),
                new CostDistributor.Input(2L, BigDecimal.ZERO, false, b("1500"))));
        assertEquals(b("250.00"), r.get(1L).amount());
        assertEquals(b("750.00"), r.get(2L).amount());
    }

    @Test
    void sharesAlwaysAddUpToTheTotal() {
        Map<Long, CostDistributor.Share> r = CostDistributor.distribute(b("100"), List.of(
                new CostDistributor.Input(1L, b("1"), true, b("1")),
                new CostDistributor.Input(2L, b("1"), true, b("1")),
                new CostDistributor.Input(3L, b("1"), true, b("1"))));
        BigDecimal sum = r.values().stream().map(CostDistributor.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(b("100.00"), sum);
    }

    @Test
    void zeroCostGivesZeroShares() {
        Map<Long, CostDistributor.Share> r = CostDistributor.distribute(BigDecimal.ZERO,
                List.of(new CostDistributor.Input(1L, b("5"), true, b("1000"))));
        assertEquals(b("0.00"), r.get(1L).amount());
    }
}
