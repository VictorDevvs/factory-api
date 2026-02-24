package com.projedata.factory.api.service;

import com.projedata.factory.api.dto.RawMaterialRequest;
import com.projedata.factory.api.entity.RawMaterial;
import com.projedata.factory.api.exception.ResourceNotFoundException;
import com.projedata.factory.api.repository.RawMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RawMaterialService {

    private final RawMaterialRepository rawMaterialRepository;

    public List<RawMaterial> findAll() {
        return rawMaterialRepository.findAll();
    }

    public RawMaterial findById(Long id) {
        return rawMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RawMaterial", id));
    }

    public RawMaterial create(RawMaterialRequest request){
        RawMaterial rawMaterial = RawMaterial.builder()
                .code(request.code())
                .name(request.name())
                .stockQuantity(request.stockQuantity())
                .unit(request.unit())
                .build();

        return  rawMaterialRepository.save(rawMaterial);
    }

    public RawMaterial update(Long id, RawMaterialRequest request) {
        RawMaterial rawMaterial = findById(id);

        rawMaterial.setCode(request.code());
        rawMaterial.setName(request.name());
        rawMaterial.setStockQuantity(request.stockQuantity());
        rawMaterial.setUnit(request.unit());

        return rawMaterialRepository.save(rawMaterial);
        }

    public void delete(Long id) {
        findById(id);
        rawMaterialRepository.deleteById(id);
    }
}
