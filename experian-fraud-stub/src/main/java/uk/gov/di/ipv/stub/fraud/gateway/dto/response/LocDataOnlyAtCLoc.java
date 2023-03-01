package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocDataOnlyAtCLoc implements Serializable {

    @JsonProperty("startDateOldestPrim")
    private String startDateOldestPrim;

    public String getStartDateOldestPrim() {
        return startDateOldestPrim;
    }

    public void setStartDateOldestPrim(String startDateOldestPrim) {
        this.startDateOldestPrim = startDateOldestPrim;
    }
}
