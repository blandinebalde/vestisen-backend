package com.vestisen.repository;

import com.vestisen.model.PublicationTarif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublicationTarifRepository extends JpaRepository<PublicationTarif, Long> {
    Optional<PublicationTarif> findByTypeNameAndActiveTrue(String typeName);
    Optional<PublicationTarif> findByTypeName(String typeName);
}
