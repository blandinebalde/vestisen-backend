package com.vendit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vendit.dto.PublicationTarifDTO;
import com.vendit.model.PublicationTarif;
import com.vendit.repository.PublicationTarifRepository;
import com.vendit.util.PublicationTarifMapper;

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
        
        List<PublicationTarifDTO> dtos = tarifs.stream()
                .map(PublicationTarifMapper::toDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
}
