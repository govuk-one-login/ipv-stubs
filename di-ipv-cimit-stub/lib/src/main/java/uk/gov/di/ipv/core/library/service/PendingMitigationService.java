package uk.gov.di.ipv.core.library.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.core.library.model.UserMitigationRequest;
import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.PendingMitigationItem;

import static uk.gov.di.ipv.core.library.config.ConfigurationVariable.CIMIT_STUB_TTL;
import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.PENDING_MITIGATIONS_TABLE;

public class PendingMitigationService {
    private static final Logger LOGGER = LogManager.getLogger();
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

    public void persistPendingMitigation(
            UserMitigationRequest userMitigationRequest, String ci, String method) {
        LOGGER.info(
                "Creating pending mitigation for request '{}', ci '{}', method '{}'",
                userMitigationRequest,
                ci,
                method);
        dataStore.create(
                PendingMitigationItem.fromMitigationRequestAndVerb(
                        userMitigationRequest, ci, method),
                CIMIT_STUB_TTL);
    }
}
