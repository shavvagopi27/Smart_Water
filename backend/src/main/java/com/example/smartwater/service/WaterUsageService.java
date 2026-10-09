package com.example.smartwater.service;

import com.example.smartwater.dto.UsageDtos.*;
import com.example.smartwater.entity.*;
import com.example.smartwater.exception.BadRequestException;
import com.example.smartwater.exception.ConflictException;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.*;
import com.example.smartwater.security.AuthUser;
import com.example.smartwater.security.SecurityUtils;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WaterUsageService {
    private final WaterUsageLogRepository logRepository;
    private final WaterMeterRepository meterRepository;
    private final HouseholdRepository householdRepository;
    private final BillingCycleRepository cycleRepository;

    // ------------------------------------------------------------------ recording

    /** Manual daily reading (POST /api/water-usage). */
    @Transactional
    public UsageLogResponse recordReading(WaterUsageRequest req) {
        Household h = householdRepository.findById(req.householdId())
                .orElseThrow(() -> new ResourceNotFoundException("Household not found"));
        WaterMeter meter = meterRepository.findById(req.meterId())
                .orElseThrow(() -> new ResourceNotFoundException("Water meter not found"));
        return Mappers.toUsage(saveReading(h, meter, req.readingDate(), req.meterReading(), ReadingSource.MANUAL));
    }

    /**
     * Validates and stores one reading.
     * Rules: meter belongs to household and is ACTIVE; date is not in the future / before installation / inside a
     * closed billing cycle; no duplicate for (meter, date); reading is not negative and not lower than the previous
     * reading (or the meter's initial reading). Consumption = reading - previous reading.
     */
    WaterUsageLog saveReading(Household household, WaterMeter meter, LocalDate date, BigDecimal reading, ReadingSource source) {
        if (!meter.getHousehold().getId().equals(household.getId())) {
            throw new BadRequestException("Meter " + meter.getMeterNumber() + " does not belong to household " + household.getId());
        }
        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BadRequestException("Meter " + meter.getMeterNumber() + " is " + meter.getStatus() + ", readings are not accepted");
        }
        if (date == null) throw new BadRequestException("Reading date is required");
        if (date.isAfter(LocalDate.now())) throw new BadRequestException("Reading date cannot be in the future");
        if (date.isBefore(meter.getInstallationDate())) throw new BadRequestException("Reading date is before the meter installation date");
        if (reading == null || reading.signum() < 0) throw new BadRequestException("Meter reading cannot be negative");
        if (logRepository.existsByMeterIdAndReadingDate(meter.getId(), date)) {
            throw new ConflictException("Duplicate reading: meter " + meter.getMeterNumber() + " already has a reading for " + date);
        }
        if (cycleRepository.existsByApartmentIdAndStatusNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                household.getApartment().getId(), CycleStatus.OPEN, date, date)) {
            throw new BadRequestException("Reading date " + date + " falls inside a finalized or archived billing cycle");
        }
        Optional<WaterUsageLog> latest = logRepository.findTopByMeterIdOrderByReadingDateDesc(meter.getId());
        if (latest.isPresent() && date.isBefore(latest.get().getReadingDate())) {
            throw new BadRequestException("Reading date must be after the latest recorded reading (" + latest.get().getReadingDate() + ")");
        }
        BigDecimal previous = latest.map(WaterUsageLog::getMeterReading).orElse(meter.getInitialReading());
        if (reading.compareTo(previous) < 0) {
            throw new BadRequestException("Current reading " + reading.toPlainString() + " is lower than the previous reading " + previous.toPlainString());
        }
        WaterUsageLog log = new WaterUsageLog();
        log.setHousehold(household);
        log.setMeter(meter);
        log.setReadingDate(date);
        log.setMeterReading(reading);
        log.setConsumption(reading.subtract(previous));
        log.setSource(source);
        log = logRepository.save(log);
        meter.setCurrentReading(reading);
        meterRepository.save(meter);
        return log;
    }

    // ------------------------------------------------------------------ CSV

    private record CsvRow(int rowNumber, WaterMeter meter, Long householdId, LocalDate date, BigDecimal reading) {}

    /** CSV columns: meterId (meter number or numeric id), householdId, readingDate (yyyy-MM-dd), meterReading. */
    @Transactional
    public CsvUploadResult uploadCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("The uploaded CSV file is empty");
        List<RowError> errors = new ArrayList<>();
        List<CsvRow> parsed = new ArrayList<>();
        int total = 0, failed = 0, duplicates = 0, success = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new BadRequestException("The uploaded CSV file is empty");
            if (headerLine.startsWith("\uFEFF")) headerLine = headerLine.substring(1);
            List<String> header = Arrays.stream(headerLine.split(",", -1)).map(s -> s.trim().toLowerCase()).toList();
            int iMeter = header.indexOf("meterid"), iHouse = header.indexOf("householdid"),
                    iDate = header.indexOf("readingdate"), iRead = header.indexOf("meterreading");
            if (iMeter < 0 || iHouse < 0 || iDate < 0 || iRead < 0) {
                throw new BadRequestException("CSV header must contain: meterId,householdId,readingDate,meterReading");
            }
            String line;
            int rowNo = 1;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (line.isBlank()) continue;
                total++;
                String[] p = line.split(",", -1);
                try {
                    if (p.length <= Math.max(Math.max(iMeter, iHouse), Math.max(iDate, iRead))) {
                        throw new BadRequestException("expected " + header.size() + " columns");
                    }
                    WaterMeter meter = findMeter(p[iMeter].trim());
                    Long householdId = parseLong(p[iHouse].trim(), "householdId");
                    LocalDate date = parseDate(p[iDate].trim());
                    BigDecimal reading = parseDecimal(p[iRead].trim());
                    parsed.add(new CsvRow(rowNo, meter, householdId, date, reading));
                } catch (BadRequestException | ResourceNotFoundException e) {
                    failed++;
                    errors.add(new RowError(rowNo, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        // Process in date order per meter so consecutive readings validate correctly.
        parsed.sort(Comparator.comparing((CsvRow r) -> r.meter().getId()).thenComparing(CsvRow::date));
        for (CsvRow row : parsed) {
            try {
                Household h = householdRepository.findById(row.householdId())
                        .orElseThrow(() -> new BadRequestException("Household " + row.householdId() + " not found"));
                saveReading(h, row.meter(), row.date(), row.reading(), ReadingSource.CSV);
                success++;
            } catch (ConflictException e) {
                duplicates++;
                errors.add(new RowError(row.rowNumber(), e.getMessage()));
            } catch (BadRequestException e) {
                failed++;
                errors.add(new RowError(row.rowNumber(), e.getMessage()));
            }
        }
        errors.sort(Comparator.comparingInt(RowError::row));
        return new CsvUploadResult(total, success, failed, duplicates, errors);
    }

    private WaterMeter findMeter(String key) {
        if (key.isEmpty()) throw new BadRequestException("meterId is empty");
        Optional<WaterMeter> byNumber = meterRepository.findByMeterNumber(key);
        if (byNumber.isPresent()) return byNumber.get();
        if (key.matches("\\d+")) {
            return meterRepository.findById(Long.valueOf(key)).orElseThrow(() -> new BadRequestException("Meter '" + key + "' not found"));
        }
        throw new BadRequestException("Meter '" + key + "' not found");
    }

    private Long parseLong(String s, String field) {
        try { return Long.valueOf(s); } catch (NumberFormatException e) { throw new BadRequestException(field + " '" + s + "' is not a number"); }
    }

    private LocalDate parseDate(String s) {
        try { return LocalDate.parse(s); } catch (DateTimeParseException e) { throw new BadRequestException("readingDate '" + s + "' must use yyyy-MM-dd"); }
    }

    private BigDecimal parseDecimal(String s) {
        try {
            BigDecimal v = new BigDecimal(s);
            if (v.signum() < 0) throw new BadRequestException("meterReading cannot be negative");
            return v;
        } catch (NumberFormatException e) {
            throw new BadRequestException("meterReading '" + s + "' is not a number");
        }
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<UsageLogResponse> history(Long householdId, LocalDate from, LocalDate to) {
        SecurityUtils.requireHouseholdAccess(householdId);
        householdRepository.findById(householdId).orElseThrow(() -> new ResourceNotFoundException("Household not found"));
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(90) : from;
        return logRepository.findByHouseholdIdAndReadingDateBetweenOrderByReadingDateAsc(householdId, start, end)
                .stream().map(Mappers::toUsage).toList();
    }

    /** Daily consumption. Residents always get their own household; admins may pick one or get the apartment total. */
    @Transactional(readOnly = true)
    public List<DailyPoint> daily(Long householdId, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        for (WaterUsageLog l : scopedLogs(householdId, start, end)) {
            byDay.merge(l.getReadingDate(), l.getConsumption(), BigDecimal::add);
        }
        return byDay.entrySet().stream().map(e -> new DailyPoint(e.getKey(), e.getValue())).toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyPoint> monthly(Long householdId, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusMonths(11).withDayOfMonth(1) : from;
        Map<YearMonth, BigDecimal> byMonth = new TreeMap<>();
        for (WaterUsageLog l : scopedLogs(householdId, start, end)) {
            byMonth.merge(YearMonth.from(l.getReadingDate()), l.getConsumption(), BigDecimal::add);
        }
        return byMonth.entrySet().stream().map(e -> new MonthlyPoint(e.getKey().toString(), e.getValue())).toList();
    }

    private List<WaterUsageLog> scopedLogs(Long householdId, LocalDate start, LocalDate end) {
        AuthUser user = SecurityUtils.currentUser();
        if (!user.isAdmin()) {
            if (user.householdId() == null) throw new BadRequestException("Your account is not assigned to a household yet");
            if (householdId != null && !householdId.equals(user.householdId())) {
                throw new AccessDeniedException("You can only access your own household");
            }
            return logRepository.findByHouseholdIdAndReadingDateBetweenOrderByReadingDateAsc(user.householdId(), start, end);
        }
        if (householdId != null) {
            return logRepository.findByHouseholdIdAndReadingDateBetweenOrderByReadingDateAsc(householdId, start, end);
        }
        List<Long> ids = householdRepository.findByActiveTrue().stream().map(Household::getId).toList();
        if (ids.isEmpty()) return List.of();
        return logRepository.findByHouseholdIdInAndReadingDateBetweenOrderByReadingDateAsc(ids, start, end);
    }
}
