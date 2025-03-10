package org.example.onlinemart.service;

import org.example.onlinemart.cache.CacheKeys;
import org.example.onlinemart.cache.CacheService;
import org.example.onlinemart.controller.AdminController;
import org.example.onlinemart.controller.AdminSummaryUtil;
import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.dto.PopularProductResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AdminSummaryService {
    private static final Logger logger = LoggerFactory.getLogger(AdminSummaryService.class);

    private final OrderService orderService;
    private final OrderItemDAO orderItemDAO;
    private final CacheService cacheService;

    @Value("${redis.cache.adminSummary.TTL:120}")
    private long adminSummaryCacheTTL;

    @Autowired
    public AdminSummaryService(OrderService orderService,
                               OrderItemDAO orderItemDAO,
                               CacheService cacheService) {
        this.orderService = orderService;
        this.orderItemDAO = orderItemDAO;
        this.cacheService = cacheService;
    }

    public AdminController.ProductStats findMostProfitableProduct() {
        String cacheKey = CacheKeys.AdminSummary.MOST_PROFITABLE;

        Optional<AdminController.ProductStats> cachedStats =
                cacheService.get(cacheKey, AdminController.ProductStats.class);

        if (cachedStats.isPresent()) {
            logger.debug("Cache hit for most profitable product");
            return cachedStats.get();
        }

        logger.debug("Cache miss for most profitable product");
        AdminController.ProductStats result =
                AdminSummaryUtil.findMostProfitableProduct(orderService, orderItemDAO);

        cacheService.set(cacheKey, result, adminSummaryCacheTTL, TimeUnit.SECONDS);

        return result;
    }

    public int countTotalSold() {
        String cacheKey = CacheKeys.AdminSummary.TOTAL_SOLD;

        Optional<Integer> cachedCount = cacheService.get(cacheKey, Integer.class);

        if (cachedCount.isPresent()) {
            logger.debug("Cache hit for total sold count");
            return cachedCount.get();
        }

        logger.debug("Cache miss for total sold count");

        int total = AdminSummaryUtil.countTotalSold(orderService, orderItemDAO);
        cacheService.set(cacheKey, total, adminSummaryCacheTTL, TimeUnit.SECONDS);

        return total;
    }

    public List<PopularProductResult> findTop3PopularProducts() {
        String cacheKey = CacheKeys.AdminSummary.topPopular(3);

        Optional<List<PopularProductResult>> cachedResults =
                cacheService.getList(cacheKey, PopularProductResult.class);

        if (cachedResults.isPresent()) {
            logger.debug("Cache hit for top 3 popular products");
            return cachedResults.get();
        }

        logger.debug("Cache miss for top 3 popular products");

        List<Object[]> rows = orderItemDAO.findTop3Popular();
        List<PopularProductResult> results = new ArrayList<>();

        for (Object[] row : rows) {
            org.example.onlinemart.entity.Product product = (org.example.onlinemart.entity.Product) row[0];
            Long totalQty = (Long) row[1];
            results.add(new PopularProductResult(
                    (long) product.getProductId(),
                    product.getProductName(),
                    totalQty
            ));
        }

        cacheService.set(cacheKey, results, adminSummaryCacheTTL, TimeUnit.SECONDS);

        return results;
    }

    /**
     * Clears all admin summary caches. Should be called after significant data changes
     * that would affect the summary statistics.
     */
    public void clearAllSummaryCaches() {
        try {
            logger.debug("Clearing all admin summary caches");
            cacheService.delete(
                    CacheKeys.AdminSummary.MOST_PROFITABLE,
                    CacheKeys.AdminSummary.TOTAL_SOLD,
                    CacheKeys.AdminSummary.topPopular(3),
                    CacheKeys.AdminSummary.REVENUE_METRICS
            );
        } catch (Exception e) {
            logger.warn("Error clearing admin summary caches", e);
            // Don't propagate the exception - cache clearing should be non-critical
        }
    }
}