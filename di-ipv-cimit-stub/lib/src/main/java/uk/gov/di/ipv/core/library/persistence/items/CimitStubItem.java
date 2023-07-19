package uk.gov.di.ipv.core.library.persistence.items;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.List;

@DynamoDbBean
@Data
public class CimitStubItem implements DynamodbItem {
    private String userId;
    private String contraIndicatorCode;

    private Instant issuanceDate;
    private long ttl;

    private List<String> mitigations;

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }

    @DynamoDbSortKey
    public String getContraIndicatorCode() {
        return contraIndicatorCode;
    }
}
