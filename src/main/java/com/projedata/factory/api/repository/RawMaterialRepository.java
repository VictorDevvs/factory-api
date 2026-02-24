package com.projedata.factory.api.repository;

import com.projedata.factory.api.entity.RawMaterial;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RawMaterialRepository extends JpaRepository<@NonNull RawMaterial, @NonNull Long> {

    boolean existsBycode(String code);
    Optional<RawMaterial> findBycode(String code);

}
