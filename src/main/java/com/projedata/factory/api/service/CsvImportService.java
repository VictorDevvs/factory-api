package com.projedata.factory.api.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.projedata.factory.api.dto.CsvImportResponse;
import com.projedata.factory.api.entity.RawMaterial;
import com.projedata.factory.api.repository.RawMaterialRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final RawMaterialRepository repository;

    @Transactional
    public CsvImportResponse importRawMaterials(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;
        int lineNumber = 1;

        try (CSVReader reader = new CSVReader(buildReader(file))) {

            reader.skip(1);
            String[] line;

            while ((line = reader.readNext()) != null) {
                lineNumber++;

                try {
                    RawMaterial material = parseLine(line, lineNumber);
                    upsert(material);
                    imported++;
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping line {}: {}", lineNumber, e.getMessage());
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    skipped++;
                }
            }

        } catch (CsvValidationException e) {
            errors.add("Invalid CSV format: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process CSV file", e);
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage());
        }

        log.info("CSV import finished — imported: {}, skipped: {}", imported, skipped);
        return new CsvImportResponse(imported, skipped, errors);
    }

    private RawMaterial parseLine(String[] columns, int lineNumber) {
        if (columns.length < 4) {
            throw new IllegalArgumentException(
                    "Expected 4 columns (code, name, stockQuantity, unit), got " + columns.length);
        }

        String code = columns[0].trim();
        String name = columns[1].trim();
        String quantityRaw = columns[2].trim();
        String unit = columns[3].trim();

        if (code.isBlank()) throw new IllegalArgumentException("Code is required");
        if (name.isBlank()) throw new IllegalArgumentException("Name is required");
        if (unit.isBlank()) throw new IllegalArgumentException("Unit is required");

        BigDecimal stockQuantity;
        try {
            stockQuantity = new BigDecimal(quantityRaw);
            if (stockQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Stock quantity must be >= 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stock quantity: '" + quantityRaw + "'");
        }

        return RawMaterial.builder()
                .code(code)
                .name(name)
                .stockQuantity(stockQuantity)
                .unit(unit)
                .build();
    }

    private void upsert(RawMaterial incoming) {
        repository.findByCode(incoming.getCode())
                .ifPresentOrElse(
                        existing -> {
                            existing.setName(incoming.getName());
                            existing.setStockQuantity(incoming.getStockQuantity());
                            existing.setUnit(incoming.getUnit());
                            repository.save(existing);
                        },
                        () -> repository.save(incoming)
                );
    }

    private InputStreamReader buildReader(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();

        if (bytes.length >= 3 &&
                bytes[0] == (byte) 0xEF &&
                bytes[1] == (byte) 0xBB &&
                bytes[2] == (byte) 0xBF) {

            return new InputStreamReader(
                    new java.io.ByteArrayInputStream(bytes, 3, bytes.length - 3),
                    StandardCharsets.UTF_8
            );
        }

        try {
            String test = new String(bytes, StandardCharsets.UTF_8);
            if (!test.contains("\uFFFD")) {
                return new InputStreamReader(
                        new java.io.ByteArrayInputStream(bytes),
                        StandardCharsets.UTF_8
                );
            }
        } catch (Exception ignored) {}

        return new InputStreamReader(
                new java.io.ByteArrayInputStream(bytes),
                StandardCharsets.ISO_8859_1
        );
    }
}
