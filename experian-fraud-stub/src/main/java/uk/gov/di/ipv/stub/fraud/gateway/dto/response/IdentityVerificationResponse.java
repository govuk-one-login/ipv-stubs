package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentityVerificationResponse implements Serializable {
    @JsonProperty("responseHeader")
    private ResponseHeader responseHeader;

    @JsonProperty("clientResponsePayload")
    private ClientResponsePayload clientResponsePayload;

    @JsonProperty("originalRequestData")
    private OriginalRequestData originalRequestData;

    public ResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(ResponseHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

    public ClientResponsePayload getClientResponsePayload() {
        return clientResponsePayload;
    }

    public void setClientResponsePayload(ClientResponsePayload clientResponsePayload) {
        this.clientResponsePayload = clientResponsePayload;
    }

    public OriginalRequestData getOriginalRequestData() {
        return originalRequestData;
    }

    public void setOriginalRequestData(OriginalRequestData originalRequestData) {
        this.originalRequestData = originalRequestData;
    }
}
