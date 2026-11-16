package com.smarttraveler.push;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;
import java.util.Properties;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PushKeys {
    private final String publicKey;
    private final String privateKey;
    private final String subject;

    public PushKeys(@Value("${app.push.public-key:}") String publicKey,
                    @Value("${app.push.private-key:}") String privateKey,
                    @Value("${app.push.subject:mailto:push@example.com}") String subject,
                    @Value("${app.push.key-file:.local/web-push/vapid.properties}") String keyFile)
            throws IOException, GeneralSecurityException {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
        if (publicKey.isBlank() != privateKey.isBlank()) {
            throw new IllegalStateException("WEB_PUSH_PUBLIC_KEY와 WEB_PUSH_PRIVATE_KEY를 함께 설정해주세요.");
        }
        if (publicKey.isBlank()) {
            Path path = Path.of(keyFile).toAbsolutePath();
            Properties keys = new Properties();
            if (Files.exists(path)) {
                try (var input = Files.newInputStream(path)) { keys.load(input); }
            } else {
                var generator = KeyPairGenerator.getInstance("ECDH", "BC");
                generator.initialize(ECNamedCurveTable.getParameterSpec("prime256v1"));
                var pair = generator.generateKeyPair();
                var encoder = Base64.getUrlEncoder().withoutPadding();
                keys.setProperty("publicKey", encoder.encodeToString(Utils.encode((ECPublicKey) pair.getPublic())));
                keys.setProperty("privateKey", encoder.encodeToString(Utils.encode((ECPrivateKey) pair.getPrivate())));
                Files.createDirectories(path.getParent());
                try (var output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
                    keys.store(output, "Local Web Push VAPID keys - do not commit or share");
                }
            }
            publicKey = keys.getProperty("publicKey", "");
            privateKey = keys.getProperty("privateKey", "");
        }
        if (!Utils.verifyKeyPair(Utils.loadPrivateKey(privateKey), Utils.loadPublicKey(publicKey))) {
            throw new IllegalStateException("Web Push 공개 키와 비밀 키가 일치하지 않습니다.");
        }
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    public String publicKey() { return publicKey; }
    String privateKey() { return privateKey; }
    String subject() { return subject; }
}
