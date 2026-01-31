package com.vestisen.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stockage des fichiers uploadés.
 * Photos annonces : annonce/user/{codeAnnonce}/filename
 */
@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;

    private static final String ANNOUNCE_RELATIVE_PREFIX = "annonce/user/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final String[] ALLOWED_EXTENSIONS = { ".jpg", ".jpeg", ".png", ".webp", ".gif" };

    /**
     * Enregistre les photos d'une annonce dans annonce/user/{annonceCode}/
     * @param annonceCode code unique de l'annonce
     * @param files fichiers images
     * @return liste des chemins relatifs pour l'URL (ex: annonce/user/ABC123/photo.jpg)
     */
    public List<String> storeAnnoncePhotos(String annonceCode, MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        Path basePath = Paths.get(uploadDir).resolve("annonce").resolve("user").resolve(annonceCode);
        Files.createDirectories(basePath);
        List<String> relativePaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) continue;
            String ext = getExtension(originalName);
            if (!isAllowedImageExtension(ext)) continue;
            if (file.getSize() > MAX_FILE_SIZE) continue;
            String safeName = UUID.randomUUID().toString().substring(0, 8) + "_" + sanitizeFileName(originalName);
            Path target = basePath.resolve(safeName);
            Files.copy(file.getInputStream(), target);
            relativePaths.add(ANNOUNCE_RELATIVE_PREFIX + annonceCode + "/" + safeName);
        }
        return relativePaths;
    }

    private static String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i).toLowerCase() : "";
    }

    private static boolean isAllowedImageExtension(String ext) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
