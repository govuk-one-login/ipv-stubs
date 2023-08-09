package uk.gov.di.ipv.core.stubmanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.stubmanagement.exceptions.BadRequestException;
import uk.gov.di.ipv.core.stubmanagement.exceptions.DataNotFoundException;
import uk.gov.di.ipv.core.stubmanagement.model.UserCisRequest;
import uk.gov.di.ipv.core.stubmanagement.model.UserMitigationRequest;
import uk.gov.di.ipv.core.stubmanagement.service.impl.CimitStubService;
import uk.gov.di.ipv.core.stubmanagement.service.impl.UserServiceImpl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.core.library.config.ConfigurationVariable.CIMIT_STUB_TTL;

@ExtendWith(MockitoExtension.class)
public class CimitStubServiceTest {

    @Mock private DataStore<CimitStubItem> mockDataStore;

    @Mock private ConfigService mockConfigService;

    @InjectMocks private CimitStubService cimitStubService;

    private static final String DB_TTL = "1800";

    @Test
    public void getCimitStubItemTest() {
        String userId = "testUserId";
        List<CimitStubItem> expectedItems = Arrays.asList(new CimitStubItem(), new CimitStubItem());

        when(mockDataStore.getItems(userId)).thenReturn(expectedItems);

        List<CimitStubItem> actualItems = cimitStubService.getCimitStubItems(userId);

        verify(mockDataStore, times(1)).getItems(userId);
        assertEquals(expectedItems, actualItems);
    }

    @Test
    public void persistCimitStubTest() {
        String userId = "testUserId";
        String contraIndicatorCode = "testCode";
        Instant issuanceDate = Instant.now();
        List<String> mitigations = Collections.singletonList("V03");

        CimitStubItem resultItem =
                cimitStubService.persistCimitStub(
                        userId, contraIndicatorCode, issuanceDate, mitigations);

        verify(mockDataStore).create(any(), eq(CIMIT_STUB_TTL));
        verify(mockDataStore, never()).update(any());

        assertEquals(userId, resultItem.getUserId());
        assertEquals(contraIndicatorCode, resultItem.getContraIndicatorCode());
        assertEquals(issuanceDate, resultItem.getIssuanceDate());
        assertEquals(mitigations, resultItem.getMitigations());
    }

    @Test
    public void updateCimitStubTest() {
        CimitStubItem cimitStubItem =
                CimitStubItem.builder()
                        .userId("user123")
                        .contraIndicatorCode("CI1")
                        .issuanceDate(Instant.now())
                        .mitigations(List.of("V01", "V02", "V03"))
                        .build();

        when(mockConfigService.getSsmParameter(eq(CIMIT_STUB_TTL))).thenReturn(DB_TTL);
        cimitStubService.updateCimitStub(cimitStubItem);

        verify(mockDataStore).update(cimitStubItem);
        verify(mockDataStore, never()).create(any(), any());
    }

    @Test
    public void addUserCisShouldPersistUserCisRequestListWhenUserDoesNotExist() {
        String userId = "456";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("CI1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01", "V03"))
                                .build());

        when(mockDataStore.getItems(userId)).thenReturn(Collections.emptyList());

        UserService userService = new UserServiceImpl(mockConfigService, cimitStubService);
        userService.addUserCis(userId, userCisRequests);

        verify(mockDataStore, times(1)).create(any(), eq(CIMIT_STUB_TTL));
    }

    @Test
    public void updateUserCisShouldThrowBadRequestExceptionWhenUserDoesNotExist() {
        String userId = "9999";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01", "V03"))
                                .build());
        UserService userService = new UserServiceImpl(mockConfigService, cimitStubService);

        assertThrows(
                BadRequestException.class,
                () -> userService.updateUserCis(userId, userCisRequests));
    }

    @Test
    public void updateUserCisShouldUpdateUserCisWhenUserExists() {
        String userId = "123";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("CI1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01", "V03"))
                                .build());

        CimitStubItem existingCimitStubItem =
                CimitStubItem.builder()
                        .userId(userId)
                        .contraIndicatorCode("CI1")
                        .issuanceDate(Instant.now())
                        .mitigations(List.of("V01", "V02", "V03"))
                        .build();

        when(mockDataStore.getItems(userId))
                .thenReturn(Collections.singletonList(existingCimitStubItem));
        when(mockConfigService.getSsmParameter(eq(CIMIT_STUB_TTL))).thenReturn(DB_TTL);

        UserService userService = new UserServiceImpl(mockConfigService, cimitStubService);
        userService.updateUserCis(userId, userCisRequests);

        verify(mockDataStore, times(1)).update(any());
    }

    @Test
    public void addUserMitigationShouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {
        String userId = "9999";
        String ci = "CI1";
        UserMitigationRequest userMitigationRequest = new UserMitigationRequest();

        when(mockDataStore.getItems(userId)).thenReturn(Collections.emptyList());
        UserService userService = new UserServiceImpl(mockConfigService, cimitStubService);

        assertThrows(
                DataNotFoundException.class,
                () -> userService.addUserMitigation(userId, ci, userMitigationRequest));
    }

    @Test
    public void addUserMitigationShouldUpdateCimitStubItemWithMitigations() {
        String userId = "123";
        String ci = "CI1";
        List<String> mitigations = List.of("V04", "V05");
        UserMitigationRequest userMitigationRequest = new UserMitigationRequest();
        userMitigationRequest.setMitigations(mitigations);

        CimitStubItem existingCimitStubItem =
                CimitStubItem.builder()
                        .userId(userId)
                        .contraIndicatorCode(ci)
                        .issuanceDate(Instant.now())
                        .mitigations(Collections.emptyList())
                        .build();

        when(mockDataStore.getItems(userId))
                .thenReturn(Collections.singletonList(existingCimitStubItem));
        when(mockConfigService.getSsmParameter(eq(CIMIT_STUB_TTL))).thenReturn(DB_TTL);

        UserService userService = new UserServiceImpl(mockConfigService, cimitStubService);
        userService.addUserMitigation(userId, ci, userMitigationRequest);

        verify(mockDataStore, times(1)).update(any());
    }

    @Test
    public void updateUserMitigationShouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {
        String userId = "9999";
        String ci = "CI1";
        UserMitigationRequest userMitigationRequest = new UserMitigationRequest();

        when(mockDataStore.getItems(userId)).thenReturn(Collections.emptyList());

        UserService userService = new UserServiceImpl(mockConfigService, cimitStubService);

        assertThrows(
                DataNotFoundException.class,
                () -> userService.updateUserMitigation(userId, ci, userMitigationRequest));
    }

    @Test
    public void updateUserMitigationShouldUpdateCimitStubItemWithMitigations() {
        String userId = "123";
        String ci = "CI1";
        List<String> mitigations = List.of("V04", "V05");
        UserMitigationRequest userMitigationRequest = new UserMitigationRequest();
        userMitigationRequest.setMitigations(mitigations);

        CimitStubItem existingCimitStubItem =
                CimitStubItem.builder()
                        .userId(userId)
                        .contraIndicatorCode(ci)
                        .issuanceDate(Instant.now())
                        .mitigations(Collections.emptyList())
                        .build();

        when(mockDataStore.getItems(userId))
                .thenReturn(Collections.singletonList(existingCimitStubItem));
        when(mockConfigService.getSsmParameter(eq(CIMIT_STUB_TTL))).thenReturn(DB_TTL);

        UserService userService = new UserServiceImpl(mockConfigService, cimitStubService);
        userService.updateUserMitigation(userId, ci, userMitigationRequest);

        verify(mockDataStore, times(1)).update(any());
    }
}
