package com.example.smartwater.service;

import com.example.smartwater.dto.AlertDtos.AlertResponse;
import com.example.smartwater.entity.*;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.*;
import com.example.smartwater.security.AuthUser;
import com.example.smartwater.security.SecurityUtils;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Alert engine. For every active household it looks at the most recent daily reading and raises:
 * THRESHOLD_EXCEEDED, HIGH_CONSUMPTION, ABNORMAL_USAGE (mean + k x std-dev) and POTENTIAL_LEAKAGE.
 * Each (household, type, usage date) is only alerted once. Wording is deliberately cautious.
 */
@Service
@RequiredArgsConstructor
public class AlertService {
    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final HouseholdRepository householdRepository;
    private final WaterUsageLogRepository usageRepository;
    private final EmailService emailService;

    @Value("${app.alerts.high-usage-threshold-kl:1.0}") private double thresholdKl;
    @Value("${app.alerts.high-usage-factor:2.0}") private double highUsageFactor;
    @Value("${app.alerts.std-dev-multiplier:2.0}") private double stdDevMultiplier;
    @Value("${app.alerts.history-window-days:30}") private int windowDays;
    @Value("${app.alerts.min-history-days:7}") private int minHistoryDays;

    // ------------------------------------------------------------------ detection

    @Transactional
    public int runDetection() {
        int created = 0;
        for (Household h : householdRepository.findByActiveTrue()) {
            try {
                created += evaluate(h);
            } catch (RuntimeException e) {
                log.error("Alert evaluation failed for household {}: {}", h.getId(), e.getMessage());
            }
        }
        log.info("Alert detection finished: {} new alert(s)", created);
        return created;
    }

    private int evaluate(Household h) {
        Optional<WaterUsageLog> latestOpt = usageRepository.findTopByHouseholdIdOrderByReadingDateDesc(h.getId());
        if (latestOpt.isEmpty()) return 0;
        WaterUsageLog latest = latestOpt.get();
        LocalDate day = latest.getReadingDate();
        if (day.isBefore(LocalDate.now().minusDays(7))) return 0; // stale data: nothing new to say

        List<WaterUsageLog> window = usageRepository.findByHouseholdIdAndReadingDateBetweenOrderByReadingDateAsc(
                h.getId(), day.minusDays(windowDays), day);
        List<BigDecimal> history = window.stream().filter(l -> l.getReadingDate().isBefore(day))
                .map(WaterUsageLog::getConsumption).toList();
        BigDecimal usage = latest.getConsumption();
        int created = 0;

        // 1) configurable threshold
        if (usage.doubleValue() > thresholdKl) {
            created += raise(h, AlertType.THRESHOLD_EXCEEDED, AlertSeverity.WARNING, day, String.format(Locale.ROOT,
                    "Daily usage of %.2f kL on %s exceeded the configured threshold of %.2f kL.", usage, day, thresholdKl));
        }

        // 2) high consumption compared to the apartment's average household
        List<Long> neighbours = householdRepository.findByApartmentIdAndActiveTrueOrderByFlatNumberAsc(h.getApartment().getId())
                .stream().map(Household::getId).toList();
        List<WaterUsageLog> aptLogs = usageRepository.findByHouseholdIdInAndReadingDateBetweenOrderByReadingDateAsc(
                neighbours, day.minusDays(windowDays), day.minusDays(1));
        if (neighbours.size() > 1 && aptLogs.size() >= minHistoryDays) {
            double aptAvg = aptLogs.stream().mapToDouble(l -> l.getConsumption().doubleValue()).average().orElse(0);
            if (aptAvg > 0 && usage.doubleValue() > highUsageFactor * aptAvg) {
                created += raise(h, AlertType.HIGH_CONSUMPTION, AlertSeverity.WARNING, day, String.format(Locale.ROOT,
                        "High water consumption: %.2f kL on %s is %.1f times the apartment's average household daily usage (%.2f kL).",
                        usage, day, usage.doubleValue() / aptAvg, aptAvg));
            }
        }

        // 3) statistical anomaly: usage > mean + k * std-dev of the household's own history
        AnomalyDetector.Result stat = AnomalyDetector.analyze(history, usage, stdDevMultiplier, minHistoryDays);
        if (stat.sufficientData() && stat.anomalous()) {
            created += raise(h, AlertType.ABNORMAL_USAGE, AlertSeverity.CRITICAL, day, String.format(Locale.ROOT,
                    "Potential abnormal usage detected. Usage of %.2f kL on %s is above this household's normal range "
                            + "(average %.2f kL, alert level %.2f kL).", usage, day, stat.mean(), stat.threshold()));
        }

        // 4) potential leakage: three consecutive days well above the household's normal level
        if (window.size() >= 3) {
            WaterUsageLog d0 = window.get(window.size() - 1), d1 = window.get(window.size() - 2), d2 = window.get(window.size() - 3);
            boolean consecutive = d1.getReadingDate().equals(d0.getReadingDate().minusDays(1))
                    && d2.getReadingDate().equals(d0.getReadingDate().minusDays(2));
            List<BigDecimal> baseline = window.stream().filter(l -> l.getReadingDate().isBefore(d2.getReadingDate()))
                    .map(WaterUsageLog::getConsumption).toList();
            if (consecutive && baseline.size() >= minHistoryDays) {
                double mean = baseline.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
                double limit = mean * 1.5;
                if (mean > 0 && d0.getConsumption().doubleValue() > limit && d1.getConsumption().doubleValue() > limit
                        && d2.getConsumption().doubleValue() > limit) {
                    created += raise(h, AlertType.POTENTIAL_LEAKAGE, AlertSeverity.CRITICAL, day, String.format(Locale.ROOT,
                            "Potential leakage pattern: usage has stayed more than 1.5x this household's normal level (%.2f kL/day) "
                                    + "for 3 consecutive days up to %s. Please check taps, toilets and pipes.", mean, day));
                }
            }
        }
        return created;
    }

    private int raise(Household h, AlertType type, AlertSeverity severity, LocalDate day, String message) {
        if (alertRepository.existsByHouseholdIdAndAlertTypeAndUsageDate(h.getId(), type, day)) return 0;
        Alert a = new Alert();
        a.setHousehold(h);
        a.setAlertType(type);
        a.setSeverity(severity);
        a.setMessage(message);
        a.setUsageDate(day);
        a = alertRepository.save(a);
        emailService.sendAlert(a);
        return 1;
    }

    // ------------------------------------------------------------------ API

    @Transactional(readOnly = true)
    public List<AlertResponse> list(Long householdId) {
        AuthUser user = SecurityUtils.currentUser();
        List<Alert> alerts;
        if (user.isAdmin()) {
            alerts = householdId == null ? alertRepository.findAllByOrderByIdDesc() : alertRepository.findByHouseholdIdOrderByIdDesc(householdId);
        } else {
            if (user.householdId() == null) return List.of();
            if (householdId != null && !householdId.equals(user.householdId())) {
                throw new AccessDeniedException("You can only access your own alerts");
            }
            alerts = alertRepository.findByHouseholdIdOrderByIdDesc(user.householdId());
        }
        return alerts.stream().map(Mappers::toAlert).toList();
    }

    @Transactional
    public AlertResponse markRead(Long id) {
        Alert a = alertRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        SecurityUtils.requireHouseholdAccess(a.getHousehold().getId());
        a.setReadFlag(true);
        return Mappers.toAlert(alertRepository.save(a));
    }
}
