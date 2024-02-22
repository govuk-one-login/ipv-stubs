package uk.gov.di.ipv.core.library.service;

import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;

import java.time.Instant;
import java.util.List;

import static uk.gov.di.ipv.core.library.config.ConfigurationVariable.CIMIT_STUB_TTL;
import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.CIMIT_STUB_TABLE_NAME;

public class CimitStubItemService {

    private final DataStore<CimitStubItem> dataStore;

    private final ConfigService configService;

    public CimitStubItemService(ConfigService configService) {
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

    public CimitStubItemService(DataStore<CimitStubItem> dataStore, ConfigService configService) {
        this.dataStore = dataStore;
        this.configService = configService;
    }

    public List<CimitStubItem> getCIsForUserId(String userId) {
        return dataStore.getItems(userId);
    }

    public List<CimitStubItem> getCiForUserId(String userId, String ci) {
        return dataStore.getItemsBySortKeyPrefix(userId, ci);
    }

    public void persistCimitStubItem(CimitStubItem cimitStubItem) {
        dataStore.create(cimitStubItem, CIMIT_STUB_TTL);
    }

    public CimitStubItem persistCimitStub(
            String userId,
            String contraIndicatorCode,
            List<String> issuers,
            Instant issuanceDate,
            List<String> mitigations) {

        CimitStubItem cimitStubItem =
                CimitStubItem.builder()
                        .userId(userId)
                        .contraIndicatorCode(contraIndicatorCode)
                        .issuers(issuers)
                        .issuanceDate(issuanceDate)
                        .mitigations(mitigations)
                        .build();

        dataStore.create(cimitStubItem, CIMIT_STUB_TTL);
        return cimitStubItem;
    }

    public void updateCimitStubItem(CimitStubItem cimitStubItem) {
        cimitStubItem.setTtl(
                Instant.now()
                        .plusSeconds(Long.parseLong(configService.getSsmParameter(CIMIT_STUB_TTL)))
                        .getEpochSecond());
        dataStore.update(cimitStubItem);
    }

    public void deleteCimitStubItem(String userId, String contraIndicatorCode) {
        dataStore.delete(userId, contraIndicatorCode);
    }
}
