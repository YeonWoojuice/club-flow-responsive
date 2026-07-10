package com.clubflow.backend.google;

import com.clubflow.backend.user.User;
import com.clubflow.backend.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleDataOAuthServiceTest {

    @Mock
    private GoogleConnectionRepository connectionRepository;

    @Mock
    private GoogleTokenCipher tokenCipher;

    @Mock
    private UserService userService;

    @Mock
    private User user;

    @Mock
    private GoogleConnection connection;

    private GoogleDataOAuthService service;

    @BeforeEach
    void setUp() {
        service = new GoogleDataOAuthService(
                connectionRepository,
                tokenCipher,
                userService,
                "client-id",
                "client-secret",
                "http://localhost/callback"
        );
    }

    @Test
    void 현재_사용자의_Google_연결을_삭제한다() {
        UUID userId = UUID.randomUUID();
        when(userService.getByGoogleSub("google-sub")).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.of(connection));

        service.disconnect("google-sub");

        verify(connectionRepository).delete(connection);
    }

    @Test
    void 이미_연결이_없어도_연결_해제를_성공으로_처리한다() {
        UUID userId = UUID.randomUUID();
        when(userService.getByGoogleSub("google-sub")).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.empty());

        service.disconnect("google-sub");

        verify(connectionRepository, never()).delete(connection);
    }
}
