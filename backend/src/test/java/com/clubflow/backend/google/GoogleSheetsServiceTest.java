package com.clubflow.backend.google;

import com.clubflow.backend.common.ConflictException;
import com.clubflow.backend.member.retention.dto.ParsedWorkbookResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;

@ExtendWith(MockitoExtension.class)
class GoogleSheetsServiceTest {

    private static final String SPREADSHEET_ID = "sheet-id";

    @Mock
    private GoogleDataOAuthService googleDataOAuthService;

    private MockRestServiceServer server;
    private GoogleSheetsService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        service = new GoogleSheetsService(googleDataOAuthService, restClientBuilder.build());
    }

    @Test
    void 한글_공백_작은따옴표가_있는_시트_이름을_한_번만_인코딩해서_읽는다() {
        String title = "26기 지원자's";
        String a1Range = "'26기 지원자''s'";
        String encodedRange = UriUtils.encodePathSegment(a1Range, StandardCharsets.UTF_8);
        URI valuesUri = URI.create(
                "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID + "/values/" + encodedRange
        );
        when(googleDataOAuthService.requireAccessToken("google-sub")).thenReturn("access-token");

        server.expect(once(), requestTo(
                        "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID
                ))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(
                        "{\"sheets\":[{\"properties\":{\"title\":\"26기 지원자's\"}}]}",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(once(), requestTo(valuesUri))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(
                        "{\"values\":[[\"이름\",\"이메일\"],[\"홍길동\",\"hong@example.com\"]]}",
                        MediaType.APPLICATION_JSON
                ));

        ParsedWorkbookResponse response = service.readTables("google-sub", SPREADSHEET_ID);

        assertThat(response.tables()).singleElement().satisfies(table -> {
            assertThat(table.name()).isEqualTo(title);
            assertThat(table.headers()).containsExactly("이름", "이메일");
            assertThat(table.rows()).containsExactly(java.util.List.of("홍길동", "hong@example.com"));
        });
        server.verify();
    }

    @Test
    void 잘못된_시트_범위는_원인을_알_수_있는_오류로_안내한다() {
        when(googleDataOAuthService.requireAccessToken("google-sub")).thenReturn("access-token");
        server.expect(once(), requestTo(
                        "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID
                ))
                .andRespond(withBadRequest().body("{\"error\":{\"message\":\"Unable to parse range\"}}"));

        assertThatThrownBy(() -> service.readTables("google-sub", SPREADSHEET_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Google Sheet의 탭 이름 또는 범위를 읽지 못했습니다.");
        server.verify();
    }
}
