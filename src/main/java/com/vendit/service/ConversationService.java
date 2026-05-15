package com.vendit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vendit.dto.ConversationDTO;
import com.vendit.dto.MessageCreateRequest;
import com.vendit.dto.MessageDTO;
import com.vendit.model.Conversation;
import com.vendit.model.Message;
import com.vendit.model.User;
import com.vendit.repository.AnnonceRepository;
import com.vendit.repository.ConversationRepository;
import com.vendit.repository.MessageRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private AnnonceRepository annonceRepository;

    public ConversationDTO getOrCreate(UUID annoncePublicId, User buyer) {
        var annonce = annonceRepository.findByPublicId(annoncePublicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        if (annonce.getSeller().getId().equals(buyer.getId())) {
            throw new RuntimeException("You cannot chat with yourself");
        }
        Conversation conv = conversationRepository.findByAnnonceIdAndUserId(annonce.getId(), buyer.getId())
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setAnnonce(annonce);
                    c.setBuyer(buyer);
                    c.setSeller(annonce.getSeller());
                    return conversationRepository.save(c);
                });
        return toDTO(conv);
    }

    public List<ConversationDTO> listMyConversations(User user) {
        return conversationRepository.findByBuyerIdOrSellerId(user.getId()).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public ConversationDTO getConversation(UUID conversationPublicId, User user) {
        Conversation conv = conversationRepository.findByPublicId(conversationPublicId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!conv.getBuyer().getId().equals(user.getId()) && !conv.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        return toDTO(conv);
    }

    public MessageDTO sendMessage(MessageCreateRequest request, User sender) {
        Conversation conv = conversationRepository.findByPublicId(request.getConversationPublicId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!conv.getBuyer().getId().equals(sender.getId()) && !conv.getSeller().getId().equals(sender.getId())) {
            throw new RuntimeException("Access denied");
        }
        Message msg = new Message();
        msg.setConversation(conv);
        msg.setSender(sender);
        msg.setContent(request.getContent());
        msg = messageRepository.save(msg);
        return toMessageDTO(msg);
    }

    private ConversationDTO toDTO(Conversation c) {
        ConversationDTO dto = new ConversationDTO();
        dto.setPublicId(c.getPublicId());
        dto.setAnnoncePublicId(c.getAnnonce().getPublicId());
        dto.setAnnonceTitle(c.getAnnonce().getTitle());
        dto.setBuyerPublicId(c.getBuyer().getPublicId());
        dto.setBuyerName(c.getBuyer().getFirstName() + " " + c.getBuyer().getLastName());
        dto.setSellerPublicId(c.getSeller().getPublicId());
        dto.setSellerName(c.getSeller().getFirstName() + " " + c.getSeller().getLastName());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setMessages(messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId())
                .stream().map(this::toMessageDTO).collect(Collectors.toList()));
        return dto;
    }

    private MessageDTO toMessageDTO(Message m) {
        MessageDTO dto = new MessageDTO();
        dto.setId(m.getId());
        dto.setConversationPublicId(m.getConversation().getPublicId());
        dto.setSenderPublicId(m.getSender().getPublicId());
        dto.setSenderName(m.getSender().getFirstName() + " " + m.getSender().getLastName());
        dto.setContent(m.getContent());
        dto.setCreatedAt(m.getCreatedAt());
        dto.setReadAt(m.getReadAt());
        return dto;
    }
}
