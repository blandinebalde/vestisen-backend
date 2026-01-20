package com.vestisen.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    public void sendVerificationEmail(String to, String token, String firstName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Vérification de votre email - VestiSen");
        message.setText(buildVerificationEmailContent(firstName, token));
        mailSender.send(message);
    }
    
    public void sendPasswordResetEmail(String to, String token, String firstName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Réinitialisation de votre mot de passe - VestiSen");
        message.setText(buildPasswordResetEmailContent(firstName, token));
        mailSender.send(message);
    }
    
    private String buildVerificationEmailContent(String firstName, String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        return String.format(
            "Bonjour %s,\n\n" +
            "Merci de vous être inscrit sur VestiSen.\n\n" +
            "Pour activer votre compte, veuillez cliquer sur le lien suivant :\n" +
            "%s\n\n" +
            "Ce lien est valide pendant 24 heures.\n\n" +
            "Si vous n'avez pas créé de compte, veuillez ignorer cet email.\n\n" +
            "Cordialement,\n" +
            "L'équipe VestiSen",
            firstName, verificationUrl
        );
    }
    
    private String buildPasswordResetEmailContent(String firstName, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        return String.format(
            "Bonjour %s,\n\n" +
            "Vous avez demandé à réinitialiser votre mot de passe.\n\n" +
            "Cliquez sur le lien suivant pour réinitialiser votre mot de passe :\n" +
            "%s\n\n" +
            "Ce lien est valide pendant 1 heure.\n\n" +
            "Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email.\n\n" +
            "Cordialement,\n" +
            "L'équipe VestiSen",
            firstName, resetUrl
        );
    }
}
