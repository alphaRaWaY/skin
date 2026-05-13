package org.skinAI.controller;

import org.skinAI.pojo.Result;
import org.skinAI.pojo.medical.DashboardSummary;
import org.skinAI.services.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Result<DashboardSummary> summary() {
        return Result.success(dashboardService.summary());
    }

    @GetMapping("/announcements")
    public Result announcements() {
        return Result.error("暂无此功能");
    }
}

