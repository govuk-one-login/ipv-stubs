package uk.gov.di.ipv.stub.orc.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record JarUserInfo(
        @JsonProperty(value = "https://vocab.account.gov.uk/v1/coreIdentityJWT")
                Essential coreIdentityJwtClaim,
        @JsonProperty(value = "https://vocab.account.gov.uk/v1/address") Essential addressClaim,
        @JsonProperty(value = "https://vocab.account.gov.uk/v1/passport") Essential passportClaim,
        @JsonProperty(value = "https://vocab.account.gov.uk/v1/socialSecurityNumber")
                Essential ninoClaim,
        @JsonInclude(JsonInclude.Include.NON_NULL)
                @JsonProperty(value = "https://vocab.account.gov.uk/v1/inheritedIdentityJWT")
                ListOfStringValues inheritedIdentityClaim,
        @JsonProperty(value = "https://vocab.account.gov.uk/v1/storageAccessToken")
                ListOfStringValues evcsAccessToken) {}
