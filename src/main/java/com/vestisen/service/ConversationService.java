package com.vestisen.service;

import com.vestisen.dto.ConversationDTO;
import com.vestisen.dto.MessageCreateRequest;
import com.vestisen.dto.MessageDTO;
import com.vestisen.model.Conversation;
import com.vestisen.model.Message;
import com.vestisen.model.User;
import com.vestisen.repository.AnnonceRepository;
import com.vestisen.repository.ConversationRepository;
import com.vestisen.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public ConversationDTO getOrCreate(Long annonceId, User buyer) {
        var annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        if (annonce.getSeller().getId().equals(buyer.getId())) {
            throw new RuntimeException("You cannot chat with yourself");
        }
        Conversation conv = conversationRepository.findByAnnonceIdAndUserId(annonceId, buyer.getId())
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

    public ConversationDTO getConversation(Long conversationId, User user) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!conv.getBuyer().getId().equals(user.getId()) && !conv.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        return toDTO(conv);
    }

    public MessageDTO sendMessage(MessageCreateRequest request, User sender) {
        Conversation conv = conversationRepository.findById(request.getConversationId())
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
        dto.setId(c.getId());
        dto.setAnnonceId(c.getAnnonce().getId());
        dto.setAnnonceTitle(c.getAnnonce().getTitle());
        dto.setBuyerId(c.getBuyer().getId());
        dto.setBuyerName(c.getBuyer().getFirstName() + " " + c.getBuyer().getLastName());
        dto.setSellerId(c.getSeller().getId());
        dto.setSellerName(c.getSeller().getFirstName() + " " + c.getSeller().getLastName());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setMessages(messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId())
                .stream().map(this::toMessageDTO).collect(Collectors.toList()));
        return dto;
    }

    private MessageDTO toMessageDTO(Message m) {
        MessageDTO dto = new MessageDTO();
        dto.setId(m.getId());
        dto.setConversationId(m.getConversation().getId());
        dto.setSenderId(m.getSender().getId());
        dto.setSenderName(m.getSender().getFirstName() + " " + m.getSender().getLastName());
        dto.setContent(m.getContent());
        dto.setCreatedAt(m.getCreatedAt());
        dto.setReadAt(m.getReadAt());
        return dto;
    }
}
