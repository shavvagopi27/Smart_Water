package com.example.smartwater.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record ComparisonPoint(String label, BigDecimal household, BigDecimal apartmentAverage) {}

    public record ResidentDashboard(String userName, Long householdId, String flatNumber,
                                    LocalDate latestReadingDate, BigDecimal latestDayConsumption,
                                    BigDecimal monthToDateConsumption, BigDecimal estimatedCurrentCharge,
                                    BigDecimal apartmentAverageMonthToDate,
                                    BillingDtos.InvoiceResponse latestInvoice, long unreadAlerts,
                                    List<AlertDtos.AlertResponse> recentAlerts,
                                    List<UsageDtos.DailyPoint> dailySeries,
                                    List<ComparisonPoint> monthlyComparison,
                                    List<TipDtos.TipResponse> tips) {}

    public record HouseholdUsage(Long householdId, String flatNumber, BigDecimal consumption) {}

    public record AdminDashboard(long totalHouseholds, BigDecimal monthConsumptionKl, BigDecimal monthPurchasedKl,
                                 BigDecimal totalBilledAmount, BillingDtos.CycleResponse currentCycle,
                                 List<HouseholdUsage> highConsumptionHouseholds,
                                 List<AlertDtos.AlertResponse> anomalies,
                                 List<ComparisonPoint> usageComparison) {}
}
