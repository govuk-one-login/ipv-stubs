package uk.gov.di.ipv.core.stubmanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.stubmanagement.exceptions.BadRequestException;
import uk.gov.di.ipv.core.stubmanagement.model.UserCisRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private CimitStubItemService cimitStubItemService;

    @InjectMocks private UserService userService;

    @Test
    public void shouldReturnSuccessFromAddUserCis() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01", "V03"))
                                .build(),
                        UserCisRequest.builder()
                                .code("code2")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(Collections.emptyList())
                                .build());

        when(cimitStubItemService.getCIsForUserId(userId)).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> userService.addUserCis(userId, userCisRequests));

        verify(cimitStubItemService, times(userCisRequests.size()))
                .persistCimitStub(any(), any(), any(), any());
        verify(cimitStubItemService, never()).updateCimitStubItem(any());
    }

    @Test
    public void shouldReturnFailedFromAddUserCisBadRequestExceptionThrown() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01"))
                                .build());

        assertThrows(
                BadRequestException.class, () -> userService.addUserCis(userId, userCisRequests));

        verify(cimitStubItemService, never()).persistCimitStub(any(), any(), any(), any());
    }

    @Test
    public void shouldReturnSuccessFromAddUserCisForUpdateScenario() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01", "V03"))
                                .build());
        List<CimitStubItem> existingItems = new ArrayList<>();
        existingItems.add(
                new CimitStubItem(userId, "code1", Instant.now(), 30000, new ArrayList<>()));

        when(cimitStubItemService.getCIsForUserId(userId)).thenReturn(existingItems);

        assertDoesNotThrow(() -> userService.addUserCis(userId, userCisRequests));

        verify(cimitStubItemService, times(1)).updateCimitStubItem(any());
    }

    @Test
    public void shouldReturnSuccessFromUpdateUserCis() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01"))
                                .build());

        List<CimitStubItem> existingItems = new ArrayList<>();
        existingItems.add(
                new CimitStubItem(userId, "code1", Instant.now(), 30000, new ArrayList<>()));

        when(cimitStubItemService.getCIsForUserId(userId)).thenReturn(existingItems);

        assertDoesNotThrow(() -> userService.updateUserCis(userId, userCisRequests));

        verify(cimitStubItemService, times(userCisRequests.size()))
                .persistCimitStub(any(), any(), any(), any());
    }

    @Test
    public void shouldReturnFailedFromUpdateUserCisBadRequestExceptionThrown() {
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
    public void shouldReturnSuccessWithCurrentDifferentCIsFromUpdateUserCis() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01"))
                                .build());

        List<CimitStubItem> existingItems =
                List.of(
                        new CimitStubItem(userId, "code1", Instant.now(), 30000, new ArrayList<>()),
                        new CimitStubItem(userId, "code2", Instant.now(), 30000, new ArrayList<>()),
                        new CimitStubItem(
                                userId, "code3", Instant.now(), 30000, new ArrayList<>()));

        when(cimitStubItemService.getCIsForUserId(userId)).thenReturn(existingItems);

        assertDoesNotThrow(() -> userService.updateUserCis(userId, userCisRequests));

        verify(cimitStubItemService, times(userCisRequests.size()))
                .persistCimitStub(any(), any(), any(), any());
    }

    @Test
    public void shouldReturnSuccessFromAddUserCisWhenExistingMitigationsIsNull() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01", "V03"))
                                .build());
        List<CimitStubItem> existingItems =
                List.of(new CimitStubItem(userId, "code1", Instant.now(), 30000, null));

        when(cimitStubItemService.getCIsForUserId(userId)).thenReturn(existingItems);

        assertDoesNotThrow(() -> userService.addUserCis(userId, userCisRequests));

        verify(cimitStubItemService, times(1)).updateCimitStubItem(any());
    }

    @Test
    public void shouldReturnSuccessFromAddUserCisWhenNewMitigationsIsNull() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(null)
                                .build());
        List<CimitStubItem> existingItems =
                List.of(
                        new CimitStubItem(
                                userId, "code1", Instant.now(), 30000, List.of("V01", "V03")));

        when(cimitStubItemService.getCIsForUserId(userId)).thenReturn(existingItems);

        assertDoesNotThrow(() -> userService.addUserCis(userId, userCisRequests));

        verify(cimitStubItemService, times(1)).updateCimitStubItem(any());
    }

    @Test
    public void shouldReturnSuccessFromAddUserCisWhenExistingAndNewMitigationsIsNull() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(null)
                                .build());
        List<CimitStubItem> existingItems =
                List.of(new CimitStubItem(userId, "code1", Instant.now(), 30000, null));

        when(cimitStubItemService.getCIsForUserId(userId)).thenReturn(existingItems);

        assertDoesNotThrow(() -> userService.addUserCis(userId, userCisRequests));

        verify(cimitStubItemService, times(1)).updateCimitStubItem(any());
    }
}