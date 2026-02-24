package com.projedata.factory.api.repository;

import com.projedata.factory.api.entity.Product;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<@NonNull Product, @NonNull Long> {

    boolean existsByCode(String code);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.compositions LEFT JOIN FETCH c.rawMaterial")
    List<Product> findAllWithCompositions();

}
