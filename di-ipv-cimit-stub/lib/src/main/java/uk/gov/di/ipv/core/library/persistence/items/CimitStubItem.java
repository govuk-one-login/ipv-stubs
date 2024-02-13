package uk.gov.di.ipv.core.library.persistence.items;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CimitStubItem implements DynamodbItem {
    private String userId;
    private String contraIndicatorCode;
    private List<String> issuers;
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

    public void addMitigations(List<String> newMitigations) {
        if (this.mitigations == null) {
            this.mitigations = newMitigations;
            return;
        }
        if (newMitigations == null) {
            return;
        }
        this.mitigations =
                Stream.concat(this.mitigations.stream(), newMitigations.stream())
                        .sorted()
                        .distinct()
                        .collect(Collectors.toList());
    }
}
