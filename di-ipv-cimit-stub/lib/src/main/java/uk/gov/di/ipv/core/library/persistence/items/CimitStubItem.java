package uk.gov.di.ipv.core.library.persistence.items;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import uk.gov.di.ipv.core.library.model.UserCisRequest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CimitStubItem implements DynamodbItem {
    private String userId;
    private String sortKey;
    private String contraIndicatorCode;
    private String issuer;
    private Instant issuanceDate;
    private long ttl;
    private List<String> mitigations;
    private String document;
    private String txn;

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }

    @DynamoDbSortKey
    public String getSortKey() {
        this.sortKey = String.format("%s#%s", contraIndicatorCode, issuanceDate);
        return sortKey;
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
                        .toList();
    }

    public static CimitStubItem fromUserCiRequest(UserCisRequest ciRequest, String userId) {
        return CimitStubItem.builder()
                .userId(userId)
                .contraIndicatorCode(ciRequest.getCode().toUpperCase())
                .issuer(ciRequest.getIssuer())
                .issuanceDate(
                        ciRequest.getIssuanceDate() == null
                                ? Instant.now()
                                : Instant.parse(ciRequest.getIssuanceDate()))
                .mitigations(listToUppercase(ciRequest.getMitigations()))
                .document(ciRequest.getDocument())
                .txn(ciRequest.getTxn())
                .build();
    }

    private static List<String> listToUppercase(List<String> codes) {
        if (codes != null) {
            return codes.stream().map(String::toUpperCase).toList();
        }
        return codes;
    }
}
