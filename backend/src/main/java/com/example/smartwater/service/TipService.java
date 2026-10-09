package com.example.smartwater.service;

import com.example.smartwater.dto.TipDtos.*;
import com.example.smartwater.entity.WaterSavingTip;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.WaterSavingTipRepository;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipService {
    private final WaterSavingTipRepository tipRepository;

    @Transactional(readOnly = true)
    public List<TipResponse> findAll() {
        return tipRepository.findAllByOrderByIdAsc().stream().map(Mappers::toTip).toList();
    }

    @Transactional
    public TipResponse create(TipRequest req) {
        WaterSavingTip t = new WaterSavingTip();
        t.setTitle(req.title().trim());
        t.setContent(req.content().trim());
        return Mappers.toTip(tipRepository.save(t));
    }

    @Transactional
    public TipResponse update(Long id, TipRequest req) {
        WaterSavingTip t = tipRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tip not found"));
        t.setTitle(req.title().trim());
        t.setContent(req.content().trim());
        return Mappers.toTip(tipRepository.save(t));
    }

    @Transactional
    public void delete(Long id) {
        if (!tipRepository.existsById(id)) throw new ResourceNotFoundException("Tip not found");
        tipRepository.deleteById(id);
    }
}
