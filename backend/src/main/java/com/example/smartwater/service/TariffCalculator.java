package com.example.smartwater.service;

import com.example.smartwater.exception.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure tiered-tariff calculator. Tiers come from the database (never hard-coded).
 *
 * <p>A tier covers the range (from, to] in kL; the top tier has to == null (open ended).
 * Example 0-10 @ 20, 10-20 @ 30, 20+ @ 40 and consumption 15 kL gives
 * 10 x 20 + 5 x 30 = 350.
 */
public final class TariffCalculator {
    private TariffCalculator() {}

    public record TierDef(int order, BigDecimal from, BigDecimal to, BigDecimal rate) {}

    public record TierCharge(int order, BigDecimal from, BigDecimal to, BigDecimal units,
                             BigDecimal rate, BigDecimal amount) {}

    public record Result(BigDecimal total, List<TierCharge> charges) {}

    public static Result calculate(List<TierDef> tiers, BigDecimal consumption) {
        if (consumption == null || consumption.signum() < 0) {
            throw new IllegalArgumentException("Consumption must be zero or positive");
        }
        List<TierDef> sorted = tiers.stream().sorted(Comparator.comparing(TierDef::from)).toList();
        List<TierCharge> charges = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (TierDef tier : sorted) {
            BigDecimal upper = tier.to() == null ? consumption : consumption.min(tier.to());
            BigDecimal units = upper.subtract(tier.from());
            if (units.signum() <= 0) {
                continue; // consumption does not reach this tier
            }
            BigDecimal amount = units.multiply(tier.rate()).setScale(2, RoundingMode.HALF_UP);
            charges.add(new TierCharge(tier.order(), tier.from(), tier.to(), units, tier.rate(), amount));
            total = total.add(amount);
        }
        return new Result(total.setScale(2, RoundingMode.HALF_UP), charges);
    }

    /** Tiers must start at 0, be contiguous, and end with one open-ended tier. */
    public static void validate(List<TierDef> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new BadRequestException("A tariff needs at least one tier");
        }
        List<TierDef> sorted = tiers.stream().sorted(Comparator.comparing(TierDef::from)).toList();
        if (sorted.get(0).from().compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException("The first tier must start at 0 kL");
        }
        for (int i = 0; i < sorted.size(); i++) {
            TierDef t = sorted.get(i);
            boolean last = i == sorted.size() - 1;
            if (last) {
                if (t.to() != null) {
                    throw new BadRequestException("The last tier must be open-ended (leave 'toKl' empty)");
                }
            } else {
                if (t.to() == null) {
                    throw new BadRequestException("Only the last tier may be open-ended");
                }
                if (t.to().compareTo(t.from()) <= 0) {
                    throw new BadRequestException("Each tier's 'toKl' must be greater than its 'fromKl'");
                }
                if (sorted.get(i + 1).from().compareTo(t.to()) != 0) {
                    throw new BadRequestException("Tiers must be contiguous: the next tier must start where the previous one ends");
                }
            }
        }
    }
}
