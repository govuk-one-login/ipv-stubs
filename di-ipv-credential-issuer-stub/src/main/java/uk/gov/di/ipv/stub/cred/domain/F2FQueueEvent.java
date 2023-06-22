package uk.gov.di.ipv.stub.cred.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class F2FQueueEvent {
    private String sub;
    private String state;

    @JsonProperty("https://vocab.account.gov.uk/v1/credentialJWT")
    private List<String> vcJwt;
}
