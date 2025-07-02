package uk.gov.di.ipv.stub.cred.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CriResponseQueueEvent {
    private String sub;
    private String state;

    @JsonProperty("govuk_signin_journey_id")
    private String journeyId = "stub-journey-id";

    @JsonProperty("https://vocab.account.gov.uk/v1/credentialJWT")
    private List<String> vcJwt;

    public CriResponseQueueEvent(String sub, String state, List<String> vcJwt) {
        this.sub = sub;
        this.state = state;
        this.vcJwt = vcJwt;
    }
}
