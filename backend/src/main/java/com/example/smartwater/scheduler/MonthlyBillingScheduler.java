package com.example.smartwater.scheduler;

import com.example.smartwater.dto.BillingDtos.CycleRequest;
import com.example.smartwater.dto.BillingDtos.CycleResponse;
import com.example.smartwater.entity.Apartment;
import com.example.smartwater.repository.ApartmentRepository;
import com.example.smartwater.repository.BillingCycleRepository;
import com.example.smartwater.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Optional (app.billing.auto-cycle.enabled=true): on the 1st of each month creates last month's billing cycle for every
 * apartment and generates DRAFT invoices. An admin still reviews and finalizes the cycle, which sends the bills.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.billing.auto-cycle.enabled", havingValue = "true")
public class MonthlyBillingScheduler {
    private static final Logger log = LoggerFactory.getLogger(MonthlyBillingScheduler.class);

    private final ApartmentRepository apartmentRepository;
    private final BillingCycleRepository cycleRepository;
    private final BillingService billingService;

    @Scheduled(cron = "${app.billing.auto-cycle.cron}")
    public void createMonthlyCycles() {
        YearMonth last = YearMonth.from(LocalDate.now().minusMonths(1));
        for (Apartment a : apartmentRepository.findAll()) {
            try {
                if (cycleRepository.existsByApartmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        a.getId(), last.atEndOfMonth(), last.atDay(1))) {
                    continue;
                }
                CycleResponse c = billingService.createCycle(new CycleRequest(a.getId(), "Water bill " + last, last.atDay(1), last.atEndOfMonth()));
                billingService.generateInvoices(c.id(), null);
                log.info("Auto-created billing cycle {} for apartment {}", c.id(), a.getId());
            } catch (RuntimeException e) {
                log.error("Automatic billing failed for apartment {}: {}", a.getId(), e.getMessage());
            }
        }
    }
}
