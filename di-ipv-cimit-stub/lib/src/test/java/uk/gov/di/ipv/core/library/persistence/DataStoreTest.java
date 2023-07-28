package uk.gov.di.ipv.core.library.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.ConfigService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataStoreTest {
    private static final String TEST_TABLE_NAME = "test-table";

    @Mock private DynamoDbEnhancedClient mockDynamoDbEnhancedClient;
    @Mock private DynamoDbTable<CimitStubItem> mockDynamoDbTable;
    @Mock private PageIterable<CimitStubItem> mockPageIterable;
    @Mock private DynamoDbIndex<CimitStubItem> mockIndex;
    @Mock private SdkIterable<Page<CimitStubItem>> mockIterable;
    @Mock private ConfigService mockConfigService;

    private CimitStubItem cimitStubItem;
    private DataStore<CimitStubItem> dataStore;

    private final long ttl = 7200;

    @BeforeEach
    void setUp() {
        when(mockDynamoDbEnhancedClient.table(
                        anyString(), ArgumentMatchers.<TableSchema<CimitStubItem>>any()))
                .thenReturn(mockDynamoDbTable);

        cimitStubItem = CimitStubItem.builder().userId("test-user").build();

        dataStore =
                new DataStore<>(
                        TEST_TABLE_NAME,
                        CimitStubItem.class,
                        mockDynamoDbEnhancedClient,
                        false,
                        mockConfigService);
    }

    @Test
    void shouldGetItemFromDynamoDbTableViaPartitionKeyAndSortKey() {
        TableDescription tableDescription =
                TableDescription.builder().tableName(TEST_TABLE_NAME).build();
        DescribeTableResponse describeTableResponse =
                DescribeTableResponse.builder().table(tableDescription).build();
        when(mockDynamoDbTable.describeTable())
                .thenReturn(
                        new DescribeTableEnhancedResponse.Builder()
                                .response(describeTableResponse)
                                .build());

        dataStore.getItem("partition-key-12345", "sort-key-12345");

        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);

        verify(mockDynamoDbEnhancedClient)
                .table(eq(TEST_TABLE_NAME), ArgumentMatchers.<TableSchema<CimitStubItem>>any());
        verify(mockDynamoDbTable).getItem(keyCaptor.capture());
        assertEquals("partition-key-12345", keyCaptor.getValue().partitionKeyValue().s());
        assertEquals("sort-key-12345", keyCaptor.getValue().sortKeyValue().get().s());
    }
}
