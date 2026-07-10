package com.clubflow.backend.google;

import com.clubflow.backend.common.ConflictException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleTokenCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsAndDecryptsTokenWithoutStoringPlaintext() {
        GoogleTokenCipher cipher = new GoogleTokenCipher(KEY);

        String encrypted = cipher.encrypt("google-access-token");

        assertThat(encrypted).doesNotContain("google-access-token");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("google-access-token");
    }

    @Test
    void rejectsInvalidEncryptionKey() {
        GoogleTokenCipher cipher = new GoogleTokenCipher("not-a-32-byte-key");

        assertThatThrownBy(() -> cipher.encrypt("token"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Google 데이터 연결 암호화 설정을 확인해 주세요.");
    }
}
