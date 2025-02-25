package org.example.onlinemart.controller;

import org.example.onlinemart.dto.PopularProductResult;
import org.example.onlinemart.service.AdminSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/summary")
public class AdminSummaryController {

    @Autowired
    private AdminSummaryService adminSummaryService;

    @GetMapping("/admin-top3-popular")
    public List<PopularProductResult> getTop3Popular() {
        return adminSummaryService.getTop3PopularProducts();
    }
}