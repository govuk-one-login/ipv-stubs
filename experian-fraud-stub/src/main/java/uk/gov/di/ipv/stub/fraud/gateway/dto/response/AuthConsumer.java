package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthConsumer implements Serializable {

    @JsonProperty("locDataOnlyAtCLoc")
    private LocDataOnlyAtCLoc locDataOnlyAtCLoc;

    @JsonProperty("idandLocDataAtCL")
    private IDandLocDataAtCL idandLocDataAtCL;

    public LocDataOnlyAtCLoc getLocDataOnlyAtCLoc() {
        return locDataOnlyAtCLoc;
    }

    public void setLocDataOnlyAtCLoc(LocDataOnlyAtCLoc locDataOnlyAtCLoc) {
        this.locDataOnlyAtCLoc = locDataOnlyAtCLoc;
    }

    public IDandLocDataAtCL getIdandLocDataAtCL() {
        return idandLocDataAtCL;
    }

    public void setIdandLocDataAtCL(IDandLocDataAtCL idandLocDataAtCL) {
        this.idandLocDataAtCL = idandLocDataAtCL;
    }
}
