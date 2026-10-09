package com.example.smartwater.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits the apartment's bulk water purchase cost between households.
 *
 * <ol>
 *   <li>Households with usable meter readings share the pool in proportion to measured consumption.</li>
 *   <li>Households WITHOUT usable readings (no meter / no readings in the cycle) are charged by flat-area:
 *       their slice of the total cost = totalCost x (their area / area of all households).
 *       The remaining cost is shared among the metered households by consumption.</li>
 *   <li>If nobody has readings, the whole cost is split by flat area.</li>
 *   <li>Amounts are rounded to 2 decimals and the rounding difference is added to the largest share
 *       so the shares always add up exactly to the total cost.</li>
 * </ol>
 */
public final class CostDistributor {
    private CostDistributor() {}

    public record Input(Long householdId, BigDecimal consumption, boolean metered, BigDecimal areaSqft) {}

    public record Share(BigDecimal amount, boolean areaFallback) {}

    public static Map<Long, Share> distribute(BigDecimal totalCost, List<Input> inputs) {
        Map<Long, BigDecimal> amounts = new HashMap<>();
        Map<Long, Boolean> fallback = new HashMap<>();
        for (Input in : inputs) {
            amounts.put(in.householdId(), BigDecimal.ZERO);
            fallback.put(in.householdId(), false);
        }
        if (inputs.isEmpty() || totalCost == null || totalCost.signum() <= 0) {
            return finish(amounts, fallback);
        }

        List<Input> metered = inputs.stream().filter(Input::metered).toList();
        List<Input> unmetered = inputs.stream().filter(i -> !i.metered()).toList();
        BigDecimal totalArea = sumArea(inputs);
        BigDecimal unmeteredArea = sumArea(unmetered);

        BigDecimal unmeteredPool = unmetered.isEmpty() ? BigDecimal.ZERO
                : metered.isEmpty() ? totalCost
                : totalCost.multiply(unmeteredArea).divide(totalArea, 10, RoundingMode.HALF_UP);
        BigDecimal meteredPool = totalCost.subtract(unmeteredPool);

        // 1) area fallback for households without usable readings
        for (Input in : unmetered) {
            amounts.put(in.householdId(), unmeteredPool.multiply(area(in)).divide(unmeteredArea, 10, RoundingMode.HALF_UP));
            fallback.put(in.householdId(), true);
        }
        // 2) proportional-to-consumption for metered households
        BigDecimal totalConsumption = metered.stream().map(Input::consumption).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!metered.isEmpty()) {
            if (totalConsumption.signum() > 0) {
                for (Input in : metered) {
                    amounts.put(in.householdId(), meteredPool.multiply(in.consumption()).divide(totalConsumption, 10, RoundingMode.HALF_UP));
                }
            } else { // everybody measured zero: split by area among metered households
                BigDecimal meteredArea = sumArea(metered);
                for (Input in : metered) {
                    amounts.put(in.householdId(), meteredPool.multiply(area(in)).divide(meteredArea, 10, RoundingMode.HALF_UP));
                    fallback.put(in.householdId(), true);
                }
            }
        }

        // 3) round to 2 decimals and push the rounding difference onto the biggest share
        Map<Long, BigDecimal> rounded = new HashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> e : amounts.entrySet()) {
            BigDecimal r = e.getValue().setScale(2, RoundingMode.HALF_UP);
            rounded.put(e.getKey(), r);
            sum = sum.add(r);
        }
        BigDecimal diff = totalCost.setScale(2, RoundingMode.HALF_UP).subtract(sum);
        if (diff.signum() != 0) {
            Long biggest = null;
            for (Map.Entry<Long, BigDecimal> e : rounded.entrySet()) {
                if (biggest == null || e.getValue().compareTo(rounded.get(biggest)) > 0
                        || (e.getValue().compareTo(rounded.get(biggest)) == 0 && e.getKey() < biggest)) {
                    biggest = e.getKey();
                }
            }
            rounded.put(biggest, rounded.get(biggest).add(diff));
        }
        return finish(rounded, fallback);
    }

    private static Map<Long, Share> finish(Map<Long, BigDecimal> amounts, Map<Long, Boolean> fallback) {
        Map<Long, Share> out = new HashMap<>();
        amounts.forEach((id, amount) -> out.put(id, new Share(amount.setScale(2, RoundingMode.HALF_UP), fallback.get(id))));
        return out;
    }

    private static BigDecimal area(Input in) {
        return in.areaSqft() == null || in.areaSqft().signum() <= 0 ? BigDecimal.ONE : in.areaSqft();
    }

    private static BigDecimal sumArea(List<Input> list) {
        return list.stream().map(CostDistributor::area).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
