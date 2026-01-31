package com.vestisen.controller;

import com.vestisen.dto.CategoryDTO;
import com.vestisen.model.Category;
import com.vestisen.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/** Endpoint public pour lister les catégories actives (création annonce, filtre catalogue). */
@RestController
@RequestMapping("/api/categories")
public class CategoriesController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getActiveCategories() {
        List<Category> categories = categoryRepository.findAll().stream()
                .filter(Category::isActive)
                .collect(Collectors.toList());
        List<CategoryDTO> dtos = categories.stream().map(c -> {
            CategoryDTO dto = new CategoryDTO();
            dto.setId(c.getId());
            dto.setName(c.getName());
            dto.setDescription(c.getDescription());
            dto.setIcon(c.getIcon());
            dto.setActive(c.isActive());
            dto.setCreatedAt(c.getCreatedAt());
            dto.setUpdatedAt(c.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
