package uk.gov.di.ipv.core.library.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.library.model.UserMitigationRequest;
import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.persistence.items.PendingMitigationItem;

import java.util.Comparator;
import java.util.List;

import static uk.gov.di.ipv.core.library.config.ConfigurationVariable.CIMIT_STUB_TTL;
import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.PENDING_MITIGATIONS_TABLE;

public class PendingMitigationService {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String DESCRIPTION = "description";
    private final DataStore<PendingMitigationItem> dataStore;

    public PendingMitigationService(ConfigService configService) {
        boolean isRunningLocally = configService.isRunningLocally();
        dataStore =
                new DataStore<>(
                        configService.getEnvironmentVariable(PENDING_MITIGATIONS_TABLE),
                        PendingMitigationItem.class,
                        DataStore.getClient(isRunningLocally),
                        isRunningLocally,
                        configService);
    }

    public PendingMitigationService(DataStore<PendingMitigationItem> dataStore) {
        this.dataStore = dataStore;
    }

    public void persistPendingMitigation(
            UserMitigationRequest userMitigationRequest, String ci, String method) {
        LOGGER.info(
                new StringMapMessage()
                        .with(DESCRIPTION, "Creating pending mitigation")
                        .with("request", userMitigationRequest.toString())
                        .with("ci", ci)
                        .with("requestMethod", method));
        dataStore.create(
                PendingMitigationItem.fromMitigationRequestAndMethod(
                        userMitigationRequest, ci, method),
                CIMIT_STUB_TTL);
    }

    public void completePendingMitigation(
            String jwtId, String userId, CimitStubItemService cimitService) {
        PendingMitigationItem pendingMitigationItem = dataStore.getItem(jwtId, false);
        if (pendingMitigationItem == null) {
            LOGGER.info(
                    new StringMapMessage()
                            .with(DESCRIPTION, "No pending mitigations found")
                            .with("jwtId", jwtId)
                            .with("userId", userId));
            return;
        }
        List<CimitStubItem> cimitItems =
                cimitService.getCiForUserId(userId, pendingMitigationItem.getMitigatedCi());
        if (cimitItems == null || cimitItems.isEmpty()) {
            LOGGER.warn(
                    new StringMapMessage()
                            .with(DESCRIPTION, "No CI found for attempted mitigation")
                            .with("jwtId", jwtId)
                            .with("userId", userId)
                            .with("ci", pendingMitigationItem.getMitigatedCi()));
            return;
        }

        CimitStubItem itemToMitigate =
                cimitItems.stream()
                        .sorted(Comparator.comparing(CimitStubItem::getIssuanceDate))
                        .reduce((first, second) -> second)
                        .orElseThrow();

        switch (pendingMitigationItem.getRequestMethod()) {
            case "PUT" -> itemToMitigate.setMitigations(pendingMitigationItem.getMitigationCodes());
            case "POST" -> itemToMitigate.addMitigations(
                    pendingMitigationItem.getMitigationCodes());
            default -> throw new IllegalArgumentException(
                    String.format(
                            "Method not supported: %s", pendingMitigationItem.getRequestMethod()));
        }

        cimitService.updateCimitStubItem(itemToMitigate);
        LOGGER.info(
                new StringMapMessage()
                        .with(DESCRIPTION, "CI mitigated")
                        .with("jwtId", jwtId)
                        .with("userId", userId)
                        .with("ci", pendingMitigationItem.getMitigatedCi()));
    }
}
