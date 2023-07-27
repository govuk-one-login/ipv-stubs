package uk.gov.di.ipv.core.library.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;

import java.util.List;

import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.CIMIT_STUB_TABLE_NAME;

public class CimitStubItemService {
    private static final Logger LOGGER = LogManager.getLogger();

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
                        isRunningLocally);
    }

    public CimitStubItemService(DataStore<CimitStubItem> dataStore, ConfigService configService) {
        this.dataStore = dataStore;
        this.configService = configService;
    }

    public List<CimitStubItem> getCIsForUserId(String userId) {
        return dataStore.getItems(userId);
    }
}
