package com.projedata.factory.api.service;

import com.projedata.factory.api.dto.CsvImportResponse;
import com.projedata.factory.api.entity.RawMaterial;
import com.projedata.factory.api.repository.RawMaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private RawMaterialRepository repository;

    @InjectMocks
    private CsvImportService service;

    @Test
    void shouldImportValidCsvSuccessfully() {
        String csv = "code,name,stockQuantity,unit\nFL001,Flour,5000,g\nSG001,Sugar,3000,g\n";
        MockMultipartFile file = mockFile(csv);

        when(repository.findByCode(any())).thenReturn(Optional.empty());

        CsvImportResponse response = service.importRawMaterials(file);

        assertThat(response.recordsImported()).isEqualTo(2);
        assertThat(response.recordsSkipped()).isEqualTo(0);
        assertThat(response.errors()).isEmpty();
        verify(repository, times(2)).save(any(RawMaterial.class));
    }

    @Test
    void shouldSkipInvalidLinesAndContinue() {
        String csv = "code,name,stockQuantity,unit\nBAD001,Bad,NOT_A_NUMBER,g\nSG001,Sugar,3000,g\n";
        MockMultipartFile file = mockFile(csv);

        when(repository.findByCode("SG001")).thenReturn(Optional.empty());

        CsvImportResponse response = service.importRawMaterials(file);

        assertThat(response.recordsImported()).isEqualTo(1);
        assertThat(response.recordsSkipped()).isEqualTo(1);
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().getFirst()).contains("Line 2");
    }

    @Test
    void shouldUpdateExistingRawMaterialOnDuplicateCode() {
        String csv = "code,name,stockQuantity,unit\nFL001,Premium Flour,8000,kg\n";
        MockMultipartFile file = mockFile(csv);

        RawMaterial existing = RawMaterial.builder()
                .id(1L).code("FL001").name("Old Flour")
                .stockQuantity(new BigDecimal("500")).unit("g")
                .build();

        when(repository.findByCode("FL001")).thenReturn(Optional.of(existing));

        CsvImportResponse response = service.importRawMaterials(file);

        assertThat(response.recordsImported()).isEqualTo(1);
        verify(repository).save(argThat(rm ->
                rm.getName().equals("Premium Flour") &&
                        rm.getStockQuantity().compareTo(new BigDecimal("8000")) == 0 &&
                        rm.getUnit().equals("kg")
        ));
    }

    @Test
    void shouldSkipLineWithMissingColumns() {
        String csv = "code,name,stockQuantity,unit\nFL001,Flour\n";
        MockMultipartFile file = mockFile(csv);

        CsvImportResponse response = service.importRawMaterials(file);

        assertThat(response.recordsSkipped()).isEqualTo(1);
        assertThat(response.errors()).hasSize(1);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldSkipLineWithBlankCode() {
        String csv = "code,name,stockQuantity,unit\n,Flour,5000,g\n";
        MockMultipartFile file = mockFile(csv);

        CsvImportResponse response = service.importRawMaterials(file);

        assertThat(response.recordsSkipped()).isEqualTo(1);
        assertThat(response.errors().getFirst()).contains("Code is required");
        verify(repository, never()).save(any());
    }

    @Test
    void shouldSkipLineWithNegativeStockQuantity() {
        String csv = "code,name,stockQuantity,unit\nFL001,Flour,-100,g\n";
        MockMultipartFile file = mockFile(csv);

        CsvImportResponse response = service.importRawMaterials(file);

        assertThat(response.recordsSkipped()).isEqualTo(1);
        assertThat(response.errors().getFirst()).contains("Stock quantity must be >= 0");
    }

    @Test
    void shouldHandleEmptyFileBody() {
        String csv = "code,name,stockQuantity,unit\n";
        MockMultipartFile file = mockFile(csv);

        CsvImportResponse response = service.importRawMaterials(file);

        assertThat(response.recordsImported()).isEqualTo(0);
        assertThat(response.recordsSkipped()).isEqualTo(0);
        assertThat(response.errors()).isEmpty();
        verify(repository, never()).save(any());
    }

    private MockMultipartFile mockFile(String content) {
        return new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                content.getBytes()
        );
    }
}