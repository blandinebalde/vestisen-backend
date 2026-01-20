package com.vestisen.repository;

import com.vestisen.model.Annonce;
import com.vestisen.model.PublicationTarif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublicationTarifRepository extends JpaRepository<PublicationTarif, Long> {
    Optional<PublicationTarif> findByPublicationTypeAndActiveTrue(Annonce.PublicationType publicationType);
    Optional<PublicationTarif> findByPublicationType(Annonce.PublicationType publicationType);
}
