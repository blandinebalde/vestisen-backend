package com.vendit.service;

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
 * Photos annonces : annonce/user/{userCode}/{annonceCode}/filename
 */
@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;

    private static final String ANNOUNCE_RELATIVE_PREFIX = "annonce/user/";
    private static final String PROFILE_RELATIVE_PREFIX = "profile/user/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final long MAX_PROFILE_SIZE = 2 * 1024 * 1024; // 2 MB pour avatar
    private static final String[] ALLOWED_EXTENSIONS = { ".jpg", ".jpeg", ".png", ".webp", ".gif" };

    /**
     * Enregistre les photos d'une annonce dans annonce/user/{userCode}/{annonceCode}/
     * @param userCode code unique du vendeur
     * @param annonceCode code unique de l'annonce
     * @param files fichiers images
     * @return liste des chemins relatifs pour l'URL (ex: annonce/user/USERCODE/ANNOUNCECODE/photo.jpg)
     */
    public List<String> storeAnnoncePhotos(String userCode, String annonceCode, MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        if (userCode == null || userCode.isBlank() || annonceCode == null || annonceCode.isBlank()) {
            throw new IllegalArgumentException("userCode et annonceCode sont requis");
        }
        Path basePath = Paths.get(uploadDir).resolve("annonce").resolve("user").resolve(userCode).resolve(annonceCode);
        Files.createDirectories(basePath);
        List<String> relativePaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) originalName = "image.jpg";
            String ext = getExtension(originalName);
            if (!isAllowedImageExtension(ext)) ext = ".jpg";
            if (file.getSize() > MAX_FILE_SIZE) continue;
            String baseName = originalName.isBlank() ? "image" : sanitizeFileName(originalName);
            if (!isAllowedImageExtension(getExtension(baseName))) baseName = baseName + ext;
            String safeName = UUID.randomUUID().toString().substring(0, 8) + "_" + baseName;
            Path target = basePath.resolve(safeName);
            Files.copy(file.getInputStream(), target);
            relativePaths.add(ANNOUNCE_RELATIVE_PREFIX + userCode + "/" + annonceCode + "/" + safeName);
        }
        return relativePaths;
    }

    /**
     * Enregistre la photo de profil d'un utilisateur dans profile/user/{userCode}/
     * @param userCode code unique de l'utilisateur
     * @param file fichier image
     * @return chemin relatif pour l'URL (ex: profile/user/ABC123/xxx.jpg) ou null si invalide
     */
    public String storeProfilePhoto(String userCode, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || userCode == null || userCode.isBlank()) {
            return null;
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) originalName = "avatar.jpg";
        String ext = getExtension(originalName);
        if (!isAllowedImageExtension(ext)) return null;
        if (file.getSize() > MAX_PROFILE_SIZE) return null;
        Path basePath = Paths.get(uploadDir).resolve("profile").resolve("user").resolve(userCode);
        Files.createDirectories(basePath);
        String safeName = "avatar_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path target = basePath.resolve(safeName);
        Files.copy(file.getInputStream(), target);
        return PROFILE_RELATIVE_PREFIX + userCode + "/" + safeName;
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
