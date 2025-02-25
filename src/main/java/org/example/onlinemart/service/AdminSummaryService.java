package org.example.onlinemart.service;

import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.dto.PopularProductResult;
import org.example.onlinemart.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminSummaryService {

    @Autowired
    private OrderItemDAO orderItemDAO;

    public List<PopularProductResult> getTop3PopularProducts() {
        List<Object[]> rows = orderItemDAO.findTop3Popular();
        List<PopularProductResult> results = new ArrayList<>();

        for (Object[] row : rows) {
            Product product = (Product) row[0];
            Long totalQty = (Long) row[1];
            results.add(new PopularProductResult(
                    (long) product.getProductId(),
                    product.getProductName(),
                    totalQty
            ));
        }

        return results;
    }
}