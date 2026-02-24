package com.projedata.factory.api.repository;

import com.projedata.factory.api.entity.ProductComposition;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCompositionRepository extends JpaRepository<@NonNull ProductComposition, @NonNull Long> {

    void deleteByProductId(Long productId);

}
