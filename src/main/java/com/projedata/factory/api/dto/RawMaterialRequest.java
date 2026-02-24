package com.projedata.factory.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RawMaterialRequest(
        @NotBlank
        String code,

        @NotBlank
        String name,

        @NotNull
        @DecimalMin(value = "0.0")
        BigDecimal stockQuantity,

        @NotBlank
        String unit
) {
}
