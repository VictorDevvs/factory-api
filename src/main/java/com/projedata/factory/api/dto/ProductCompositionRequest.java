package com.projedata.factory.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductCompositionRequest(
        @NotNull
        Long rawMaterialId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal requiredQuantity
) {
}
