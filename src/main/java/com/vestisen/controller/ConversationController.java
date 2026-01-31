package com.vestisen.controller;

import com.vestisen.dto.ConversationDTO;
import com.vestisen.dto.MessageCreateRequest;
import com.vestisen.dto.MessageDTO;
import com.vestisen.model.User;
import com.vestisen.repository.UserRepository;
import com.vestisen.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/annonce/{annonceId}")
    public ResponseEntity<ConversationDTO> getOrCreate(@PathVariable Long annonceId, Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(conversationService.getOrCreate(annonceId, user));
    }

    @GetMapping
    public ResponseEntity<List<ConversationDTO>> listMine(Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(conversationService.listMyConversations(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> get(@PathVariable Long id, Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(conversationService.getConversation(id, user));
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
