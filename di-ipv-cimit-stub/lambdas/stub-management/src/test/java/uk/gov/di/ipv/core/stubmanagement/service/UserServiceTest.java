package uk.gov.di.ipv.core.stubmanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.model.UserCisRequest;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.stubmanagement.exceptions.BadRequestException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private CimitStubItemService cimitStubItemService;
    @InjectMocks private UserService userService;
    @Captor private ArgumentCaptor<CimitStubItem> cimitStubItemArgumentCaptor;

    @Test
    void shouldReturnSuccessFromAddUserCis() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .issuer("https://issuer.example.com")
                                .mitigations(List.of("V01", "V03"))
                                .document("document/this/that")
                                .build(),
                        UserCisRequest.builder()
                                .code("code2")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .issuer("https://issuer.example.com")
                                .mitigations(Collections.emptyList())
                                .build());

        assertDoesNotThrow(() -> userService.addUserCis(userId, userCisRequests));

        verify(cimitStubItemService, times(userCisRequests.size()))
                .persistCimitStubItem(cimitStubItemArgumentCaptor.capture());

        List<CimitStubItem> allCapturedCimitStubItems = cimitStubItemArgumentCaptor.getAllValues();

        assertEquals(
                CimitStubItem.fromUserCiRequest(userCisRequests.get(0), userId),
                allCapturedCimitStubItems.get(0));
        assertEquals(
                CimitStubItem.fromUserCiRequest(userCisRequests.get(1), userId),
                allCapturedCimitStubItems.get(1));
    }

    @Test
    void shouldReturnFailedFromAddUserCisBadRequestExceptionThrown() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01"))
                                .build());

        assertThrows(
                BadRequestException.class, () -> userService.addUserCis(userId, userCisRequests));

        verify(cimitStubItemService, never()).persistCimitStubItem(any());
    }

    @Test
    void shouldReturnSuccessFromUpdateUserCis() {
        String userId = "user123";
        UserCisRequest userCisRequest =
                UserCisRequest.builder()
                        .code("code1")
                        .issuer("https://issuer.example.com")
                        .issuanceDate("2023-07-25T10:00:00Z")
                        .mitigations(List.of("V01"))
                        .build();

        List<CimitStubItem> existingItems = new ArrayList<>();
        existingItems.add(
                CimitStubItem.builder()
                        .userId(userId)
                        .contraIndicatorCode("CODE1")
                        .issuanceDate(Instant.now())
                        .ttl(30000)
                        .mitigations(new ArrayList<>())
                        .build());

        when(cimitStubItemService.getCIsForUserId(userId)).thenReturn(existingItems);

        assertDoesNotThrow(() -> userService.updateUserCis(userId, List.of(userCisRequest)));

        verify(cimitStubItemService).persistCimitStubItem(cimitStubItemArgumentCaptor.capture());

        assertEquals(
                CimitStubItem.fromUserCiRequest(userCisRequest, userId),
                cimitStubItemArgumentCaptor.getValue());
    }

    @Test
    void shouldReturnFailedFromUpdateUserCisBadRequestExceptionThrown() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01"))
                                .build());

        assertThrows(
                BadRequestException.class,
                () -> userService.updateUserCis(userId, userCisRequests));

        verify(cimitStubItemService, never()).updateCimitStubItem(any());
    }

    @Test
    void shouldReturnSuccessWithCurrentDifferentCIsFromUpdateUserCis() {
        String userId = "user123";
        UserCisRequest userCisRequest =
                UserCisRequest.builder()
                        .code("code1")
                        .issuanceDate("2023-07-25T10:00:00Z")
                        .mitigations(List.of("V01"))
                        .build();

        List<CimitStubItem> existingItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(userId)
                                .contraIndicatorCode("CODE1")
                                .issuanceDate(Instant.now())
                                .ttl(30000)
                                .mitigations(new ArrayList<>())
                                .build(),
                        CimitStubItem.builder()
                                .userId(userId)
                                .contraIndicatorCode("CODE2")
                                .issuanceDate(Instant.now())
                                .ttl(30000)
                                .mitigations(new ArrayList<>())
                                .build(),
                        CimitStubItem.builder()
                                .userId(userId)
                                .contraIndicatorCode("CODE3")
                                .issuanceDate(Instant.now())
                                .ttl(30000)
                                .mitigations(new ArrayList<>())
                                .build());

        when(cimitStubItemService.getCIsForUserId(userId)).thenReturn(existingItems);

        assertDoesNotThrow(() -> userService.updateUserCis(userId, List.of(userCisRequest)));

        verify(cimitStubItemService).persistCimitStubItem(cimitStubItemArgumentCaptor.capture());

        assertEquals(
                CimitStubItem.fromUserCiRequest(userCisRequest, userId),
                cimitStubItemArgumentCaptor.getValue());
    }
}
