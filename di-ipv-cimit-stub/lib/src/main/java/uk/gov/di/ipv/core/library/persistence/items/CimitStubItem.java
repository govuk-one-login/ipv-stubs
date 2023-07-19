package uk.gov.di.ipv.core.library.persistence.items;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;

import java.time.Instant;

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
    @DynamoDbSecondaryPartitionKey(indexNames = "contraIndicatorCode")
    public String getContraIndicatorCode() {
        return contraIndicatorCode;
    }
}