package uk.gov.di.ipv.stub.orc.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfo {
    @JsonProperty("https://vocab.account.gov.uk/v1/coreIdentityJWT")
    private String coreIdentityJWT;

    @JsonProperty("https://vocab.account.gov.uk/v1/socialSecurityNumber")
    private String socialSecurityNumber;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("https://vocab.account.gov.uk/v1/inheritedIdentityJWT")
    private InheritedIdentityJWT inheritedIdentityJWT;

    public void setInheritedIdentityJWT(InheritedIdentityJWT inheritedIdentityJWT) {
        this.inheritedIdentityJWT = inheritedIdentityJWT;
    }

    @Override
    public String toString() {
        return "UserInfo{"
                + "coreIdentityJWT='"
                + coreIdentityJWT
                + '\''
                + ", socialSecurityNumber='"
                + socialSecurityNumber
                + '\''
                + ", inheritedIdentityJWT="
                + inheritedIdentityJWT
                + '}';
    }
}
