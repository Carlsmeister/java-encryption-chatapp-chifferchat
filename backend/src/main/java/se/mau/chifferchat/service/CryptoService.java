package se.mau.chifferchat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.repository.UserRepository;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CryptoService {
    private final UserRepository userRepository;
    private final Map<String, String> publicKeyCache = new ConcurrentHashMap<>();

    @Transactional
    public void storePublicKey(String username, String publicKeyPem) {
        if (!validatePublicKey(publicKeyPem)) {
            throw new IllegalArgumentException("Invalid public key format");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPublicKeyPem(publicKeyPem);
        userRepository.save(user);
        cachePublicKey(username, publicKeyPem);
    }

    @Transactional(readOnly = true)
    public String getPublicKey(String username) {
        String cached = publicKeyCache.get(username);
        if (cached != null) {
            return cached;
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        cachePublicKey(username, user.getPublicKeyPem());
        return user.getPublicKeyPem();
    }

    public boolean validatePublicKey(String publicKeyPem) {
        try {
            String normalized = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(normalized);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);
            return publicKey.getEncoded().length > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    void cachePublicKey(String username, String publicKeyPem) {
        if (username != null && publicKeyPem != null) {
            publicKeyCache.put(username, publicKeyPem);
        }
    }
}
