package com.vestisen.controller;

import com.vestisen.dto.PublicationTarifDTO;
import com.vestisen.model.PublicationTarif;
import com.vestisen.repository.PublicationTarifRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tarifs")
public class TarifController {
    
    @Autowired
    private PublicationTarifRepository tarifRepository;
    
    @GetMapping
    public ResponseEntity<List<PublicationTarifDTO>> getActiveTarifs() {
        List<PublicationTarif> tarifs = tarifRepository.findAll()
                .stream()
                .filter(t -> t.isActive())
                .collect(Collectors.toList());
        
        List<PublicationTarifDTO> dtos = tarifs.stream().map(t -> {
            PublicationTarifDTO dto = new PublicationTarifDTO();
            dto.setId(t.getId());
            dto.setPublicationType(t.getPublicationType());
            dto.setPrice(t.getPrice());
            dto.setDurationDays(t.getDurationDays());
            dto.setActive(t.isActive());
            return dto;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
}
