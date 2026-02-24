package com.projedata.factory.api.service;

import com.projedata.factory.api.dto.ProductionSuggestionResponse;
import com.projedata.factory.api.entity.Product;
import com.projedata.factory.api.entity.RawMaterial;
import com.projedata.factory.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductionOptimizationService {

    private final ProductRepository productRepository;

    public ProductionSuggestionResponse optimize(){
        List<Product> products = productRepository.findAllWithCompositions();

        Map<Long, BigDecimal> availableStock = buildStockSnapshot(products);

        List<Product> sorted = products.stream()
                .sorted(Comparator.comparing(Product::getSaleValue).reversed())
                .toList();

        List<ProductionSuggestionResponse.ProductionItem> items = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        for(Product product : sorted){
            int maxUnits = calculateMaxProducible(product, availableStock);

            if (maxUnits > 0){
                consumeStock(product, maxUnits, availableStock);

                BigDecimal itemTotal = product.getSaleValue()
                        .multiply(BigDecimal.valueOf(maxUnits));

                items.add(new ProductionSuggestionResponse.ProductionItem(
                        product.getCode(),
                        product.getName(),
                        maxUnits,
                        product.getSaleValue(),
                        itemTotal
                ));

                totalValue = totalValue.add(itemTotal);
            }
        }
        return new ProductionSuggestionResponse(items, totalValue);
    }

    private Map<Long, BigDecimal> buildStockSnapshot(List<Product> products){
        Map<Long, BigDecimal> stock = new HashMap<>();
        products.forEach(p -> p.getCompositions().forEach(c -> {
            RawMaterial rawMaterial = c.getRawMaterial();
            stock.putIfAbsent(rawMaterial.getId(), rawMaterial.getStockQuantity());
        }));

        return stock;
    }

    int calculateMaxProducible(Product product, Map<Long, BigDecimal> stock){
        if (product.getCompositions().isEmpty()) return 0;

        return product.getCompositions().stream()
                .mapToInt(c -> {
                    BigDecimal available = stock.getOrDefault(
                            c.getRawMaterial().getId(), BigDecimal.ZERO
                    );
                    if (c.getRequiredQuantity().compareTo(BigDecimal.ZERO) == 0) return 0;
                    return available.divideToIntegralValue(c.getRequiredQuantity()).intValue();
                })
                .min()
                .orElse(0);
    }

    private void consumeStock(Product product, int units, Map<Long, BigDecimal> stock){
        product.getCompositions().forEach(c -> {
            BigDecimal consumed = c.getRequiredQuantity()
                    .multiply(BigDecimal.valueOf(units));
            stock.merge(c.getRawMaterial().getId(), consumed.negate(), BigDecimal::add);
        });
    }
}
