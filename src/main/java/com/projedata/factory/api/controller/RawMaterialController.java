package com.projedata.factory.api.controller;

import com.projedata.factory.api.dto.CsvImportResponse;
import com.projedata.factory.api.dto.RawMaterialRequest;
import com.projedata.factory.api.entity.RawMaterial;
import com.projedata.factory.api.service.CsvImportService;
import com.projedata.factory.api.service.RawMaterialService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/raw-materials")
@RequiredArgsConstructor
public class RawMaterialController {

    private final RawMaterialService service;
    private final CsvImportService csvImportService;

    @GetMapping
    public ResponseEntity<@NonNull List<RawMaterial>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<@NonNull RawMaterial> findById(@PathVariable Long id) {
        return  ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<@NonNull RawMaterial> create(@Valid @RequestBody RawMaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/import/simple")
    public ResponseEntity<@NonNull CsvImportResponse> importSimple(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        CsvImportResponse response = csvImportService.importRawMaterials(file);

        HttpStatus status = response.hasErrors()
                ? HttpStatus.MULTI_STATUS
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }


    @PutMapping("/{id}")
    public ResponseEntity<@NonNull RawMaterial> update(@PathVariable Long id, @Valid @RequestBody RawMaterialRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
