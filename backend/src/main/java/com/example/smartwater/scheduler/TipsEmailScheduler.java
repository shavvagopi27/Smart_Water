package com.example.smartwater.scheduler;

import com.example.smartwater.entity.Role;
import com.example.smartwater.entity.WaterSavingTip;
import com.example.smartwater.repository.UserRepository;
import com.example.smartwater.repository.WaterSavingTipRepository;
import com.example.smartwater.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Optional weekly e-mail with three random water-saving tips (app.tips.email-enabled=true). */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.tips.email-enabled", havingValue = "true")
public class TipsEmailScheduler {
    private final UserRepository userRepository;
    private final WaterSavingTipRepository tipRepository;
    private final EmailService emailService;

    @Scheduled(cron = "${app.tips.cron}")
    public void sendWeeklyTips() {
        List<WaterSavingTip> all = new ArrayList<>(tipRepository.findAllByOrderByIdAsc());
        if (all.isEmpty()) return;
        Collections.shuffle(all);
        List<WaterSavingTip> pick = all.subList(0, Math.min(3, all.size()));
        userRepository.findByRoleOrderByIdAsc(Role.RESIDENT).stream()
                .filter(u -> u.isEnabled())
                .forEach(u -> emailService.sendTips(u, pick));
    }
}
