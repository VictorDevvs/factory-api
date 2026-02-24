package com.projedata.factory.api.service;

import com.projedata.factory.api.dto.ProductCompositionRequest;
import com.projedata.factory.api.dto.ProductRequest;
import com.projedata.factory.api.entity.Product;
import com.projedata.factory.api.entity.ProductComposition;
import com.projedata.factory.api.entity.RawMaterial;
import com.projedata.factory.api.exception.ResourceNotFoundException;
import com.projedata.factory.api.repository.ProductRepository;
import com.projedata.factory.api.repository.RawMaterialRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RawMaterialRepository rawMaterialRepository;

    public List<Product> findAll() {
        return productRepository.findAllWithCompositions();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional
    public Product create(ProductRequest request){
        Product product = Product.builder()
                .code(request.code())
                .name(request.name())
                .saleValue(request.saleValue())
                .compositions(new ArrayList<>())
                .build();

        mappingCompositions(request.compositions(), product);
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductRequest request){
        Product product = findById(id);

        product.setCode(request.code());
        product.setName(request.name());
        product.setSaleValue(request.saleValue());
        product.getCompositions().clear();

        mappingCompositions(request.compositions(), product);
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        productRepository.deleteById(id);
    }

    private void mappingCompositions(@NotEmpty List<ProductCompositionRequest> compositions, Product product) {

        compositions.forEach(r -> {
            RawMaterial material = rawMaterialRepository.findById(r.rawMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("RawMaterial", r.rawMaterialId()));

            ProductComposition composition = ProductComposition.builder()
                    .product(product)
                    .rawMaterial(material)
                    .requiredQuantity(r.requiredQuantity())
                    .build();

            product.getCompositions().add(composition);
        });
    }
}
