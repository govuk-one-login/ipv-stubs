package uk.gov.di.ipv.core.stubmanagement.service.impl;

import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.ConfigService;

import java.time.Instant;
import java.util.List;

import static uk.gov.di.ipv.core.library.config.ConfigurationVariable.CIMIT_STUB_TTL;
import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.CIMIT_STUB_TABLE_NAME;

public class CimitStubService {
    private final DataStore<CimitStubItem> dataStore;
    private final ConfigService configService;

    public CimitStubService(ConfigService configService) {
        this.configService = configService;
        boolean isRunningLocally = this.configService.isRunningLocally();
        dataStore =
                new DataStore<>(
                        this.configService.getEnvironmentVariable(CIMIT_STUB_TABLE_NAME),
                        CimitStubItem.class,
                        DataStore.getClient(isRunningLocally),
                        isRunningLocally,
                        configService);
    }

    public CimitStubService(DataStore<CimitStubItem> dataStore, ConfigService configService) {
        this.dataStore = dataStore;
        this.configService = configService;
    }

    public List<CimitStubItem> getCimitStubItems(String userId) {
        return dataStore.getItems(userId);
    }

    public CimitStubItem persistCimitStub(
            String userId,
            String contraIndicatorCode,
            Instant issuanceDate,
            List<String> mitigations) {

        CimitStubItem cimitStubItem =
                CimitStubItem.builder()
                        .userId(userId)
                        .contraIndicatorCode(contraIndicatorCode)
                        .issuanceDate(issuanceDate)
                        .mitigations(mitigations)
                        .build();

        dataStore.create(cimitStubItem, CIMIT_STUB_TTL);
        return cimitStubItem;
    }

    public void updateCimitStub(CimitStubItem cimitStubItem) {
        dataStore.update(cimitStubItem);
    }
}
