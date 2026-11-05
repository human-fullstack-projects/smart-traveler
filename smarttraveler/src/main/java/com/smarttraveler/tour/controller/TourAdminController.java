package com.smarttraveler.tour.controller;

import com.smarttraveler.tour.service.TourSyncService;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 관리자 · 관광지 데이터 수집 (ADM-17 외부 데이터 수집 · WBS 관광지 연관·중심 정보 연동). */
@Controller
@RequestMapping("/admin/tour-data")
public class TourAdminController {
    private final TourSyncService sync;

    public TourAdminController(TourSyncService sync) { this.sync = sync; }

    @GetMapping
    public String page(@RequestParam(defaultValue = "47130") String region, Model model) {
        List<Map<String, Object>> regions = sync.supportedRegions();
        model.addAttribute("regions", regions);
        model.addAttribute("region", region);
        model.addAttribute("counts", sync.counts(region));
        model.addAttribute("hubs", sync.hubTop(region, 20));
        model.addAttribute("logs", sync.recentLogs());
        return "admin/tour-data";
    }

    @PostMapping("/sync")
    public String run(@RequestParam String region, RedirectAttributes redirect) {
        TourSyncService.SyncResult result = sync.sync(region);
        long failed = result.steps().stream().filter(s -> !"SUCCESS".equals(s.status())).count();
        String summary = result.regionName() + " 수집 " + (failed == 0 ? "완료" : "일부 미반영(" + failed + "단계)")
                + (result.baseYm() == null ? "" : " · 기준 연월 " + result.baseYm());
        redirect.addFlashAttribute(failed == 0 ? "message" : "error", summary);
        redirect.addAttribute("region", region);
        return "redirect:/admin/tour-data";
    }
}
