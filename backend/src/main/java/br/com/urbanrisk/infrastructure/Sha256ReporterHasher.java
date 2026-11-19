package br.com.urbanrisk.infrastructure;

import br.com.urbanrisk.application.port.ReporterHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256(pepper + contact), hex. Fails fast at startup without a pepper. */
@Component
public class Sha256ReporterHasher implements ReporterHasher {

    private final String pepper;

    public Sha256ReporterHasher(@Value("${app.reporter-hash-pepper:}") String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("REPORTER_HASH_PEPPER must be set");
        }
        this.pepper = pepper;
    }

    @Override
    public String hash(String normalizedContact) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((pepper + normalizedContact).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
