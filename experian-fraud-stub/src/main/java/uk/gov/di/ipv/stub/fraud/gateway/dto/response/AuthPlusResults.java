package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthPlusResults implements Serializable {

    @JsonProperty("authConsumer")
    private AuthConsumer authConsumer;

    public AuthConsumer getAuthConsumer() {
        return authConsumer;
    }

    public void setAuthConsumer(AuthConsumer authConsumer) {
        this.authConsumer = authConsumer;
    }
}
