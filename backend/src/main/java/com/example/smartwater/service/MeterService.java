package com.example.smartwater.service;

import com.example.smartwater.dto.MeterDtos.*;
import com.example.smartwater.entity.Household;
import com.example.smartwater.entity.MeterStatus;
import com.example.smartwater.entity.WaterMeter;
import com.example.smartwater.exception.BadRequestException;
import com.example.smartwater.exception.ConflictException;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.WaterMeterRepository;
import com.example.smartwater.repository.WaterUsageLogRepository;
import com.example.smartwater.security.AuthUser;
import com.example.smartwater.security.SecurityUtils;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeterService {
    private final WaterMeterRepository meterRepository;
    private final WaterUsageLogRepository usageLogRepository;
    private final HouseholdService householdService;

    @Transactional
    public MeterResponse create(MeterRequest req) {
        Household h = householdService.get(req.householdId());
        if (meterRepository.existsByMeterNumber(req.meterNumber().trim())) {
            throw new ConflictException("Meter number already exists");
        }
        if (meterRepository.existsByHouseholdId(h.getId())) {
            throw new ConflictException("This household already has a water meter");
        }
        WaterMeter m = new WaterMeter();
        m.setHousehold(h);
        m.setMeterNumber(req.meterNumber().trim());
        m.setInstallationDate(req.installationDate());
        m.setStatus(req.status() == null ? MeterStatus.ACTIVE : req.status());
        m.setInitialReading(req.initialReading());
        m.setCurrentReading(req.initialReading());
        return Mappers.toMeter(meterRepository.save(m));
    }

    /** Admin: all meters. Resident: the meter of their own household. */
    @Transactional(readOnly = true)
    public List<MeterResponse> findAll() {
        AuthUser user = SecurityUtils.currentUser();
        if (user.isAdmin()) {
            return meterRepository.findAll().stream().map(Mappers::toMeter).toList();
        }
        if (user.householdId() == null) return List.of();
        return meterRepository.findByHouseholdId(user.householdId()).stream().map(Mappers::toMeter).toList();
    }

    @Transactional(readOnly = true)
    public MeterResponse findById(Long id) {
        WaterMeter m = get(id);
        SecurityUtils.requireHouseholdAccess(m.getHousehold().getId());
        return Mappers.toMeter(m);
    }

    @Transactional
    public MeterResponse update(Long id, MeterRequest req) {
        WaterMeter m = get(id);
        if (!m.getHousehold().getId().equals(req.householdId())) {
            throw new BadRequestException("A meter cannot be moved to another household");
        }
        if (!m.getMeterNumber().equals(req.meterNumber().trim()) && meterRepository.existsByMeterNumber(req.meterNumber().trim())) {
            throw new ConflictException("Meter number already exists");
        }
        boolean hasReadings = usageLogRepository.existsByMeterId(id);
        if (hasReadings && m.getInitialReading().compareTo(req.initialReading()) != 0) {
            throw new BadRequestException("The initial reading cannot be changed after readings were recorded");
        }
        m.setMeterNumber(req.meterNumber().trim());
        m.setInstallationDate(req.installationDate());
        if (req.status() != null) m.setStatus(req.status());
        if (!hasReadings) {
            m.setInitialReading(req.initialReading());
            m.setCurrentReading(req.initialReading());
        }
        return Mappers.toMeter(meterRepository.save(m));
    }

    public WaterMeter get(Long id) {
        return meterRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Water meter not found"));
    }
}
