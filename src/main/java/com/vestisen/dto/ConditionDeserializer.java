package com.vestisen.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.vestisen.model.Annonce;

import java.io.IOException;

/**
 * Désérialiseur Jackson pour Annonce.Condition.
 * Accepte null et chaîne vide ("") et les convertit en null pour éviter l'erreur
 * "Cannot coerce empty String to Condition value" lorsque le frontend envoie condition: "".
 */
public class ConditionDeserializer extends JsonDeserializer<Annonce.Condition> {

    @Override
    public Annonce.Condition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Annonce.Condition.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
