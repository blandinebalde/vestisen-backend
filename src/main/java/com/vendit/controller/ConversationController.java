package com.vendit.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.vendit.dto.ConversationDTO;
import com.vendit.dto.MessageCreateRequest;
import com.vendit.dto.MessageDTO;
import com.vendit.model.User;
import com.vendit.repository.UserRepository;
import com.vendit.service.ConversationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@PreAuthorize("hasAuthority('perm:conversation:use')")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/annonce/{annoncePublicId}")
    public ResponseEntity<ConversationDTO> getOrCreate(@PathVariable UUID annoncePublicId, Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(conversationService.getOrCreate(annoncePublicId, user));
    }

    @GetMapping
    public ResponseEntity<List<ConversationDTO>> listMine(Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(conversationService.listMyConversations(user));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<ConversationDTO> get(@PathVariable UUID publicId, Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(conversationService.getConversation(publicId, user));
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageDTO> sendMessage(@Valid @RequestBody MessageCreateRequest request, Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(conversationService.sendMessage(request, user));
    }

    private User getCurrentUser(Authentication auth) {
        UserDetails ud = (UserDetails) auth.getPrincipal();
        return userRepository.findByEmailOrPhone(ud.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
