package com.projedata.factory.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.projedata.factory.api.dto.ProductionSuggestionResponse;
import com.projedata.factory.api.entity.Product;
import com.projedata.factory.api.entity.ProductComposition;
import com.projedata.factory.api.entity.RawMaterial;
import com.projedata.factory.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionOptimizationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductionOptimizationService service;

    @Test
    void shouldSuggestProductWithHighestValue(){
        RawMaterial flour = buildMaterial(1L, "Flour", new BigDecimal("1000"));

        Product bread = buildProduct(1L, "Bread", new BigDecimal("5.00"), List.of(
                buildComposition(flour, new BigDecimal("100"))
        ));

        Product cake = buildProduct(2L, "Cake", new BigDecimal("20.00"), List.of(
                buildComposition(flour, new BigDecimal("300"))
        ));

        when(productRepository.findAllWithCompositions()).thenReturn(List.of(bread, cake));

        ProductionSuggestionResponse response = service.optimize();
        assertThat(response.totalValue()).isEqualByComparingTo(new BigDecimal("65.00"));
        assertThat(response.suggestions()).hasSize(2);
    }

    @Test
    void shouldReturnEmptyWhenNoStock(){
        RawMaterial flour = buildMaterial(1L, "Flour", BigDecimal.ZERO);

        Product bread = buildProduct(1L, "Bread", new BigDecimal("5.00"), List.of(
                buildComposition(flour, new BigDecimal("100"))
        ));

        when(productRepository.findAllWithCompositions()).thenReturn(List.of(bread));

        ProductionSuggestionResponse response = service.optimize();
        assertThat(response.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.suggestions()).isEmpty();
    }

    @Test
    void shouldCalculateMaxProducibleCorrectly(){
        RawMaterial flour = buildMaterial(1L, "Flour", new BigDecimal("500"));
        Product bread = buildProduct(1L, "Bread", new BigDecimal("5.00"), List.of(
                buildComposition(flour, new BigDecimal("200"))
        ));

        Map<Long, BigDecimal> stock = Map.of(1L, new BigDecimal("500"));

        int result = service.calculateMaxProducible(bread, new HashMap<>(stock));
        assertThat(result).isEqualTo(2);
    }

    private RawMaterial buildMaterial(Long id, String name, BigDecimal stock){

        return RawMaterial.builder()
                .id(id)
                .name(name)
                .stockQuantity(stock)
                .build();
    }

    private Product buildProduct(Long id, String name, BigDecimal value, List<ProductComposition> compositions){
        return Product.builder()
                .id(id)
                .name(name)
                .saleValue(value)
                .compositions(compositions)
                .build();
    }

    private ProductComposition buildComposition(RawMaterial material, BigDecimal quantity){
        return ProductComposition.builder()
                .rawMaterial(material)
                .requiredQuantity(quantity)
                .build();
    }

}