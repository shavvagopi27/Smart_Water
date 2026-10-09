package com.example.smartwater.service;

import com.example.smartwater.dto.UsageDtos.UsageLogResponse;
import com.example.smartwater.dto.UsageDtos.WaterUsageRequest;
import com.example.smartwater.entity.*;
import com.example.smartwater.exception.BadRequestException;
import com.example.smartwater.exception.ConflictException;
import com.example.smartwater.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WaterUsageServiceTest {
    @Mock WaterUsageLogRepository logRepository;
    @Mock WaterMeterRepository meterRepository;
    @Mock HouseholdRepository householdRepository;
    @Mock BillingCycleRepository cycleRepository;

    WaterUsageService service;
    Household household;
    WaterMeter meter;
    final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new WaterUsageService(logRepository, meterRepository, householdRepository, cycleRepository);
        Apartment apt = new Apartment();
        apt.setId(1L);
        household = new Household();
        household.setId(10L);
        household.setApartment(apt);
        meter = new WaterMeter();
        meter.setId(100L);
        meter.setMeterNumber("WM-1");
        meter.setHousehold(household);
        meter.setStatus(MeterStatus.ACTIVE);
        meter.setInstallationDate(today.minusYears(1));
        meter.setInitialReading(new BigDecimal("1200"));
        meter.setCurrentReading(new BigDecimal("1200"));
        when(householdRepository.findById(10L)).thenReturn(Optional.of(household));
        when(meterRepository.findById(100L)).thenReturn(Optional.of(meter));
        when(logRepository.save(any(WaterUsageLog.class))).thenAnswer(i -> {
            WaterUsageLog l = i.getArgument(0);
            l.setId(1L);
            return l;
        });
    }

    @Test
    void consumptionIsCurrentMinusPreviousReading() {
        WaterUsageLog previous = new WaterUsageLog();
        previous.setReadingDate(today.minusDays(2));
        previous.setMeterReading(new BigDecimal("1200"));
        when(logRepository.findTopByMeterIdOrderByReadingDateDesc(100L)).thenReturn(Optional.of(previous));

        UsageLogResponse r = service.recordReading(new WaterUsageRequest(10L, 100L, today.minusDays(1), new BigDecimal("1215")));

        assertEquals(new BigDecimal("15"), r.consumption());
        assertEquals(new BigDecimal("1215"), meter.getCurrentReading());
    }

    @Test
    void firstReadingIsComparedWithInitialMeterReading() {
        when(logRepository.findTopByMeterIdOrderByReadingDateDesc(100L)).thenReturn(Optional.empty());
        UsageLogResponse r = service.recordReading(new WaterUsageRequest(10L, 100L, today.minusDays(1), new BigDecimal("1203.5")));
        assertEquals(new BigDecimal("3.5"), r.consumption());
    }

    @Test
    void readingLowerThanPreviousIsRejected() {
        WaterUsageLog previous = new WaterUsageLog();
        previous.setReadingDate(today.minusDays(2));
        previous.setMeterReading(new BigDecimal("1215"));
        when(logRepository.findTopByMeterIdOrderByReadingDateDesc(100L)).thenReturn(Optional.of(previous));

        assertThrows(BadRequestException.class,
                () -> service.recordReading(new WaterUsageRequest(10L, 100L, today.minusDays(1), new BigDecimal("1210"))));
        verify(logRepository, never()).save(any());
    }

    @Test
    void duplicateReadingIsRejectedWithConflict() {
        when(logRepository.existsByMeterIdAndReadingDate(100L, today.minusDays(1))).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> service.recordReading(new WaterUsageRequest(10L, 100L, today.minusDays(1), new BigDecimal("1300"))));
    }

    @Test
    void futureAndNegativeReadingsAreRejected() {
        assertThrows(BadRequestException.class,
                () -> service.recordReading(new WaterUsageRequest(10L, 100L, today.plusDays(1), new BigDecimal("1300"))));
        assertThrows(BadRequestException.class,
                () -> service.recordReading(new WaterUsageRequest(10L, 100L, today.minusDays(1), new BigDecimal("-5"))));
    }

    @Test
    void meterOfAnotherHouseholdIsRejected() {
        Household other = new Household();
        other.setId(11L);
        meter.setHousehold(other);
        assertThrows(BadRequestException.class,
                () -> service.recordReading(new WaterUsageRequest(10L, 100L, today.minusDays(1), new BigDecimal("1300"))));
    }

    @Test
    void inactiveMeterIsRejected() {
        meter.setStatus(MeterStatus.FAULTY);
        assertThrows(BadRequestException.class,
                () -> service.recordReading(new WaterUsageRequest(10L, 100L, today.minusDays(1), new BigDecimal("1300"))));
    }
}
