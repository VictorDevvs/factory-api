package com.projedata.factory.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductionSuggestionResponse(
        List<ProductionItem> suggestions,
        BigDecimal totalValue
) {

    public record ProductionItem(
            String productCode,
            String productName,
            int quantityToProduce,
            BigDecimal unitValue,
            BigDecimal totalItemValue
    ){}
}
