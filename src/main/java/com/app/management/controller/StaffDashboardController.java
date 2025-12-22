package com.app.management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffDashboardController {
    
    @GetMapping("/dashboard")
    public String staffDashboard(Model model) {
        model.addAttribute("pageTitle", "Staff Dashboard");
        model.addAttribute("pageSubtitle", "Operational System");
        model.addAttribute("userRole", "staff");
        model.addAttribute("dashboardUrl", "/staff/dashboard");
        model.addAttribute("backUrl", "/main/back");
        return "access/staff-dashboard";
    }
}