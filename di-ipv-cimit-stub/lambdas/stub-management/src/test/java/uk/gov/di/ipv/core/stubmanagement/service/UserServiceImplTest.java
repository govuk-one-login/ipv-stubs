package uk.gov.di.ipv.core.stubmanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.stubmanagement.exceptions.DataAlreadyExistException;
import uk.gov.di.ipv.core.stubmanagement.exceptions.DataNotFoundException;
import uk.gov.di.ipv.core.stubmanagement.model.UserCisRequest;
import uk.gov.di.ipv.core.stubmanagement.model.UserMitigationRequest;
import uk.gov.di.ipv.core.stubmanagement.service.impl.CimitStubService;
import uk.gov.di.ipv.core.stubmanagement.service.impl.UserServiceImpl;

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
public class UserServiceImplTest {

    @Mock private ConfigService configService;

    @Mock private CimitStubService cimitStubService;

    @InjectMocks private UserServiceImpl userService;

    @Test
    public void shouldReturnSuccessFromAddUserCis() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuenceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01", "V03"))
                                .build(),
                        UserCisRequest.builder()
                                .code("code2")
                                .issuenceDate("2023-07-25T10:00:00Z")
                                .mitigations(Collections.emptyList())
                                .build());

        when(cimitStubService.getCimitStubItem(userId)).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> userService.addUserCis(userId, userCisRequests));

        verify(cimitStubService, times(userCisRequests.size()))
                .persistCimitStub(any(), any(), any(), any());
    }

    @Test
    public void shouldReturnFailedFromAddUserCisUserAlreadyExistsExceptionThrown() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuenceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01", "V03"))
                                .build());
        List<CimitStubItem> existingItems = new ArrayList<>();
        existingItems.add(
                new CimitStubItem(userId, "code2", Instant.now(), 30000, new ArrayList<>()));

        when(cimitStubService.getCimitStubItem(userId)).thenReturn(existingItems);

        assertThrows(
                DataAlreadyExistException.class,
                () -> userService.addUserCis(userId, userCisRequests));
        verify(cimitStubService, never()).persistCimitStub(any(), any(), any(), any());
    }

    @Test
    public void shouldReturnSuccessFromUpdateUserCis() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuenceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01"))
                                .build());

        List<CimitStubItem> existingItems = new ArrayList<>();
        existingItems.add(
                new CimitStubItem(userId, "code1", Instant.now(), 30000, new ArrayList<>()));

        when(cimitStubService.getCimitStubItem(userId)).thenReturn(existingItems);

        assertDoesNotThrow(() -> userService.updateUserCis(userId, userCisRequests));

        verify(cimitStubService, times(1)).updateCimitStub(any());
    }

    @Test
    public void shouldReturnFailedFromUpdateUserCis() {
        String userId = "user123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuenceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01"))
                                .build());

        List<CimitStubItem> existingItems = new ArrayList<>();
        existingItems.add(
                new CimitStubItem(userId, "code3", Instant.now(), 30000, new ArrayList<>()));

        when(cimitStubService.getCimitStubItem(userId)).thenReturn(existingItems);

        assertThrows(
                DataNotFoundException.class,
                () -> userService.updateUserCis(userId, userCisRequests));

        verify(cimitStubService, never()).updateCimitStub(any());
    }

    @Test
    public void shouldReturnSuccessAddUserMitigation() {
        String userId = "user123";
        String ci = "code1";
        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(Collections.emptyList()).build();

        List<CimitStubItem> existingItems = new ArrayList<>();
        existingItems.add(new CimitStubItem(userId, ci, Instant.now(), 30000, new ArrayList<>()));

        when(cimitStubService.getCimitStubItem(userId)).thenReturn(existingItems);

        assertDoesNotThrow(() -> userService.addUserMitigation(userId, ci, userMitigationRequest));

        verify(cimitStubService, times(1)).updateCimitStub(any());
    }

    @Test
    public void shouldReturnFailedFromAddUserMitigationUserNotFoundExceptionThrown() {
        String userId = "user123";
        String ci = "code1";
        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(List.of("V01", "V03")).build();

        List<CimitStubItem> existingItems = new ArrayList<>();

        when(cimitStubService.getCimitStubItem(userId)).thenReturn(existingItems);

        assertThrows(
                DataNotFoundException.class,
                () -> userService.addUserMitigation(userId, ci, userMitigationRequest));
        verify(cimitStubService, never()).updateCimitStub(any());
    }

    @Test
    public void shouldReturnSuccessFromUpdateUserMitigation() {
        String userId = "user123";
        String ci = "code1";
        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(List.of("V01", "V03")).build();

        List<CimitStubItem> existingItems = new ArrayList<>();
        existingItems.add(new CimitStubItem(userId, ci, Instant.now(), 30000, new ArrayList<>()));

        when(cimitStubService.getCimitStubItem(userId)).thenReturn(existingItems);

        assertDoesNotThrow(
                () -> userService.updateUserMitigation(userId, ci, userMitigationRequest));

        verify(cimitStubService, times(1)).updateCimitStub(any());
    }

    @Test
    public void shouldReturnFailedFromUpdateUserMitigationUserNotFoundExceptionThrown() {
        String userId = "user123";
        String ci = "code1";
        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(List.of("V01", "V03")).build();

        List<CimitStubItem> existingItems = new ArrayList<>();

        when(cimitStubService.getCimitStubItem(userId)).thenReturn(existingItems);

        assertThrows(
                DataNotFoundException.class,
                () -> userService.updateUserMitigation(userId, ci, userMitigationRequest));
        verify(cimitStubService, never()).updateCimitStub(any());
    }
}
