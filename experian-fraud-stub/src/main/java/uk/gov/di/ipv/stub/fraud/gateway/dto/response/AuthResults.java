package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResults implements Serializable {

    @JsonProperty("authPlusResults")
    private AuthPlusResults authPlusResults;

    public AuthPlusResults getAuthPlusResults() {
        return authPlusResults;
    }

    public void setAuthPlusResults(AuthPlusResults authPlusResults) {
        this.authPlusResults = authPlusResults;
    }
}
