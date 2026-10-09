package com.example.smartwater.service;

import com.example.smartwater.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TariffCalculatorTest {
    private static BigDecimal b(String s) { return new BigDecimal(s); }

    private final List<TariffCalculator.TierDef> tiers = List.of(
            new TariffCalculator.TierDef(1, b("0"), b("10"), b("20")),
            new TariffCalculator.TierDef(2, b("10"), b("20"), b("30")),
            new TariffCalculator.TierDef(3, b("20"), null, b("40")));

    @Test
    void fifteenKilolitresCosts350() {
        TariffCalculator.Result r = TariffCalculator.calculate(tiers, b("15"));
        assertEquals(b("350.00"), r.total());
        assertEquals(2, r.charges().size());
        assertEquals(b("200.00"), r.charges().get(0).amount());
        assertEquals(b("150.00"), r.charges().get(1).amount());
    }

    @Test
    void topTierIsOpenEnded() {
        assertEquals(b("700.00"), TariffCalculator.calculate(tiers, b("25")).total()); // 200 + 300 + 5x40
    }

    @Test
    void zeroConsumptionCostsNothing() {
        assertEquals(b("0.00"), TariffCalculator.calculate(tiers, BigDecimal.ZERO).total());
    }

    @Test
    void negativeConsumptionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> TariffCalculator.calculate(tiers, b("-1")));
    }

    @Test
    void validationRejectsGapsAndClosedTopTier() {
        assertDoesNotThrow(() -> TariffCalculator.validate(tiers));
        assertThrows(BadRequestException.class, () -> TariffCalculator.validate(List.of(
                new TariffCalculator.TierDef(1, b("0"), b("10"), b("20")),
                new TariffCalculator.TierDef(2, b("12"), null, b("30")))));        // gap 10-12
        assertThrows(BadRequestException.class, () -> TariffCalculator.validate(List.of(
                new TariffCalculator.TierDef(1, b("0"), b("10"), b("20")))));      // top tier closed
        assertThrows(BadRequestException.class, () -> TariffCalculator.validate(List.of(
                new TariffCalculator.TierDef(1, b("5"), null, b("20")))));         // does not start at 0
    }
}
