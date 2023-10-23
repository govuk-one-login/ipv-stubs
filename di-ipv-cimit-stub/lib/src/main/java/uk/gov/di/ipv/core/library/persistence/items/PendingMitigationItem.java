package uk.gov.di.ipv.core.library.persistence.items;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import uk.gov.di.ipv.core.library.model.UserMitigationRequest;

import java.util.List;

@DynamoDbBean
@Data
public class PendingMitigationItem implements DynamodbItem {
    private String vcJti;
    private String mitigatedCi;
    private List<String> mitigationCodes;
    private String requestMethod;
    private long ttl;

    @DynamoDbPartitionKey
    public String getVcJti() {
        return this.vcJti;
    }

    public static PendingMitigationItem fromMitigationRequestAndVerb(
            UserMitigationRequest request, String ci, String method) {
        PendingMitigationItem item = new PendingMitigationItem();
        item.setVcJti(request.getVcJti());
        item.setMitigatedCi(ci);
        item.setMitigationCodes(request.getMitigations());
        item.setRequestMethod(method);
        return item;
    }
}
