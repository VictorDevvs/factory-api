package com.projedata.factory.api.controller;

import com.projedata.factory.api.dto.ProductionSuggestionResponse;
import com.projedata.factory.api.service.ProductionOptimizationService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionOptimizationService service;

    @GetMapping("/optimize")
    public ResponseEntity<@NonNull ProductionSuggestionResponse> optimize(){
        return ResponseEntity.ok(service.optimize());
    }
}
