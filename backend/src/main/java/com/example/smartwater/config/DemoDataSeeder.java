package com.example.smartwater.config;

import com.example.smartwater.dto.ApartmentDtos.ApartmentRequest;
import com.example.smartwater.dto.BillingDtos.CycleRequest;
import com.example.smartwater.dto.BillingDtos.CycleResponse;
import com.example.smartwater.dto.HouseholdDtos.HouseholdRequest;
import com.example.smartwater.dto.HouseholdDtos.HouseholdResponse;
import com.example.smartwater.dto.MeterDtos.MeterRequest;
import com.example.smartwater.dto.MeterDtos.MeterResponse;
import com.example.smartwater.dto.PurchaseDtos.PurchaseRequest;
import com.example.smartwater.dto.TariffDtos.TariffRequest;
import com.example.smartwater.dto.TariffDtos.TierRequest;
import com.example.smartwater.dto.UsageDtos.WaterUsageRequest;
import com.example.smartwater.dto.UserDtos.ResidentRequest;
import com.example.smartwater.entity.Role;
import com.example.smartwater.entity.SupplyType;
import com.example.smartwater.entity.User;
import com.example.smartwater.repository.UserRepository;
import com.example.smartwater.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Random;

/**
 * LOCAL / DEMO DATA ONLY (app.seed.enabled=true). Runs once on an empty database.
 * Demo credentials created here are documented in the README and must never be used in production.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    public static final String DEMO_ADMIN_EMAIL = "admin@smartwater.local";
    public static final String DEMO_ADMIN_PASSWORD = "Admin@12345";          // DEMO ONLY
    public static final String DEMO_RESIDENT_PASSWORD = "Resident@12345";    // DEMO ONLY

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApartmentService apartmentService;
    private final HouseholdService householdService;
    private final UserService userService;
    private final MeterService meterService;
    private final TariffService tariffService;
    private final WaterUsageService usageService;
    private final WaterPurchaseService purchaseService;
    private final BillingService billingService;
    private final AlertService alertService;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains users - demo data not seeded");
            return;
        }
        log.warn("Seeding LOCAL DEMO DATA (disable with SEED_DEMO_DATA=false)");

        User admin = new User();
        admin.setName("Apartment Admin");
        admin.setEmail(DEMO_ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(DEMO_ADMIN_PASSWORD));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        Long aptId = apartmentService.create(new ApartmentRequest("Green Valley Residency", "12 Lake View Road", "Bengaluru")).id();

        tariffService.create(new TariffRequest(aptId, "Standard Residential Tariff", LocalDate.now().minusYears(1), true, List.of(
                new TierRequest(BigDecimal.ZERO, BigDecimal.TEN, new BigDecimal("20")),
                new TierRequest(BigDecimal.TEN, new BigDecimal("20"), new BigDecimal("30")),
                new TierRequest(new BigDecimal("20"), null, new BigDecimal("40")))));

        String[] flats = {"A-101", "A-102", "A-103", "A-104", "A-105"};
        int[] sizes = {2, 3, 4, 4, 5};
        String[] areas = {"850", "1000", "1200", "1250", "1400"};
        String[] names = {"Asha Verma", "Rahul Nair", "Priya Sharma", "Karan Mehta", "Meera Iyer"};

        LocalDate today = LocalDate.now();
        LocalDate firstDay = YearMonth.from(today.minusMonths(1)).atDay(1);
        LocalDate lastDay = today.minusDays(1);
        Random rnd = new Random(42);

        for (int i = 0; i < flats.length; i++) {
            HouseholdResponse h = householdService.create(new HouseholdRequest(aptId, flats[i], sizes[i], new BigDecimal(areas[i]), true));
            userService.addResident(h.id(), new ResidentRequest(names[i], "resident" + (i + 1) + "@smartwater.local", DEMO_RESIDENT_PASSWORD, "+91-98765000" + (10 + i)));
            BigDecimal start = new BigDecimal(1200 + i * 150);
            MeterResponse m = meterService.create(new MeterRequest("WM-100" + (i + 1), h.id(), firstDay.minusMonths(1), null, start));

            BigDecimal reading = start;
            double base = 0.15 * sizes[i];
            for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
                double v = base * (0.85 + 0.30 * rnd.nextDouble());
                if (i == 3 && d.equals(lastDay)) v = base * 4;                      // sudden spike -> threshold/anomaly alerts
                if (i == 4 && !d.isBefore(lastDay.minusDays(2))) v = base * 2.2;    // 3 high days -> potential leakage pattern
                reading = reading.add(BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP));
                usageService.recordReading(new WaterUsageRequest(h.id(), m.id(), d, reading));
            }
        }

        LocalDate prevStart = YearMonth.from(today.minusMonths(1)).atDay(1);
        LocalDate prevEnd = YearMonth.from(today.minusMonths(1)).atEndOfMonth();
        LocalDate monthStart = today.withDayOfMonth(1);
        purchaseService.create(new PurchaseRequest(aptId, null, "City Water Board", SupplyType.MUNICIPAL, prevStart.plusDays(2), new BigDecimal("60"), new BigDecimal("25"), null));
        purchaseService.create(new PurchaseRequest(aptId, null, "Blue Drop Tankers", SupplyType.TANKER, prevStart.plusDays(14), new BigDecimal("30"), new BigDecimal("90"), null));
        purchaseService.create(new PurchaseRequest(aptId, null, "Blue Drop Tankers", SupplyType.TANKER, monthStart, new BigDecimal("20"), new BigDecimal("95"), null));

        CycleResponse prev = billingService.createCycle(new CycleRequest(aptId, "Water bill " + YearMonth.from(prevStart), prevStart, prevEnd));
        billingService.generateInvoices(prev.id(), null);
        billingService.finalizeCycle(prev.id());

        CycleResponse cur = billingService.createCycle(new CycleRequest(aptId, "Water bill " + YearMonth.from(monthStart),
                monthStart, YearMonth.from(monthStart).atEndOfMonth()));
        billingService.generateInvoices(cur.id(), null);

        alertService.runDetection();
        log.warn("Demo data ready. Admin: {} / {}  |  Residents: resident1..5@smartwater.local / {}",
                DEMO_ADMIN_EMAIL, DEMO_ADMIN_PASSWORD, DEMO_RESIDENT_PASSWORD);
    }
}
