package uk.gov.di.ipv.stub.cred.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CriResponseQueueErrorEvent {
    private String sub;
    private String state;

    @JsonProperty("govuk_signin_journey_id")
    private String journeyId = "stub-journey-id";

    private String error;
    private String error_description;

    public CriResponseQueueErrorEvent(String sub, String state, String error, String error_description) {
        this.sub = sub;
        this.state = state;
        this.error = error;
        this.error_description = error_description;
    }
}
