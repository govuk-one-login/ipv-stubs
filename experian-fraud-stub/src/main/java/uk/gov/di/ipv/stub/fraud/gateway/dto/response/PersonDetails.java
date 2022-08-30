package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonDetails implements Serializable {

    @JsonProperty("dateOfBirth")
    private String dateOfBirth;

    private String pepsSanctionsFlag;

    private String yearOfBirth;

    public String getPepsSanctionsFlag() {
        return pepsSanctionsFlag;
    }

    public void setPepsSanctionsFlag(String pepsSanctionsFlag) {
        this.pepsSanctionsFlag = pepsSanctionsFlag;
    }

    public String getYearOfBirth() {
        return yearOfBirth;
    }

    public void setYearOfBirth(String yearOfBirth) {
        this.yearOfBirth = yearOfBirth;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
