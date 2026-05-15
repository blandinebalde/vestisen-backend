package com.vendit.security;

import com.vendit.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Sceau HMAC des attributs de privilège (rôle, activation, email vérifié, version de jeton).
 * Toute modification directe en base sans recalcul du sceau est détectée à la prochaine authentification.
 */
@Service
public class PrivilegeSealService {

    private static final Logger log = LoggerFactory.getLogger(PrivilegeSealService.class);
    private static final String HMAC_ALGO = "HmacSHA256";

    @Value("${app.security.privilege-seal-secret:}")
    private String dedicatedSecret;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String effectiveSecret() {
        if (StringUtils.hasText(dedicatedSecret)) {
            return dedicatedSecret;
        }
        return jwtSecret;
    }

    private byte[] hmacKeyBytes() {
        try {
            byte[] raw = effectiveSecret().getBytes(StandardCharsets.UTF_8);
            if (raw.length >= 32) {
                return Arrays.copyOf(raw, 32);
            }
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot derive privilege seal key", e);
        }
    }

    /**
     * Données signées : tout changement de rôle / activation / vérification email / version JWT casse le sceau
     * si la ligne est modifiée hors application.
     */
    public String sealMaterial(User user) {
        if (user.getEmail() == null || user.getRole() == null) {
            return "";
        }
        return user.getEmail()
                + "|" + user.getRole().name()
                + "|" + user.getTokenVersion()
                + "|" + user.isEnabled()
                + "|" + user.isEmailVerified();
    }

    public String computeExpectedSeal(User user) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(hmacKeyBytes(), HMAC_ALGO));
            byte[] sig = mac.doFinal(sealMaterial(user).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(sig);
        } catch (Exception e) {
            throw new IllegalStateException("Privilege seal computation failed", e);
        }
    }

    public void applySeal(User user) {
        user.setPrivilegeSeal(computeExpectedSeal(user));
    }

    /**
     * @return false si le sceau est absent ou ne correspond pas au matériel courant (falsification probable).
     */
    public boolean verifySeal(User user) {
        String stored = user.getPrivilegeSeal();
        if (!StringUtils.hasText(stored)) {
            return false;
        }
        String expected = computeExpectedSeal(user);
        byte[] a = stored.getBytes(StandardCharsets.UTF_8);
        byte[] b = expected.getBytes(StandardCharsets.UTF_8);
        boolean ok = MessageDigest.isEqual(a, b);
        if (!ok) {
            log.warn("Privilege seal mismatch for user id={}, email={}", user.getId(), user.getEmail());
        }
        return ok;
    }
}
