package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnonceValidationResponseDTO {
    private boolean valid;
    private String step;
    private Map<String, String> errors = new LinkedHashMap<>();
    private Map<String, String> warnings = new LinkedHashMap<>();

    public static AnnonceValidationResponseDTO ok(String step) {
        AnnonceValidationResponseDTO dto = new AnnonceValidationResponseDTO();
        dto.setValid(true);
        dto.setStep(step);
        return dto;
    }

    public static AnnonceValidationResponseDTO fail(String step, Map<String, String> errors) {
        AnnonceValidationResponseDTO dto = new AnnonceValidationResponseDTO();
        dto.setValid(false);
        dto.setStep(step);
        dto.setErrors(errors != null ? errors : new LinkedHashMap<>());
        return dto;
    }

    public void addError(String field, String message) {
        if (errors == null) {
            errors = new LinkedHashMap<>();
        }
        errors.put(field, message);
        valid = false;
    }
}
