package org.example.onlinemart.service;

import com.google.gson.Gson;
import org.example.onlinemart.controller.AdminController;
import org.example.onlinemart.controller.AdminSummaryUtil;
import org.example.onlinemart.dao.OrderItemDAO;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import org.springframework.stereotype.Service;
import org.example.onlinemart.controller.AdminController.ProductStats;

@Service
public class AdminSummaryService {

    private final OrderService orderService;
    private final OrderItemDAO orderItemDAO;
    private final Jedis jedis;

    @Autowired
    public AdminSummaryService(OrderService orderService,
                               OrderItemDAO orderItemDAO,
                               Jedis jedis) {
        this.orderService = orderService;
        this.orderItemDAO = orderItemDAO;
        this.jedis = jedis;
    }

    public ProductStats findMostProfitableProduct() {
        String cacheKey = "summary:mostProfit";

        String cachedJson = jedis.get(cacheKey);
        if (cachedJson != null) {
            return fromJson(cachedJson, ProductStats.class);
        }

        ProductStats result = AdminSummaryUtil.findMostProfitableProduct(orderService, orderItemDAO);

        jedis.setex(cacheKey, 60, toJson(result));

        return result;
    }

    public int countTotalSold() {
        String cacheKey = "summary:totalSold";
        String cached = jedis.get(cacheKey);
        if (cached != null) {
            return Integer.parseInt(cached);
        }

        int total = AdminSummaryUtil.countTotalSold(orderService, orderItemDAO);
        jedis.setex(cacheKey, 30, String.valueOf(total));
        return total;
    }

    private String toJson(Object obj) {
        return new Gson().toJson(obj);
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        return new Gson().fromJson(json, clazz);
    }
}
