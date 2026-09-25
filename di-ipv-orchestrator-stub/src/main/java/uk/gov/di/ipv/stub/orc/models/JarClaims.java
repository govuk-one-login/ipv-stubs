package uk.gov.di.ipv.stub.orc.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record JarClaims(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty(value = "update_identity")
        Boolean updateIdentity,
        JarUserInfo userinfo) {

    public JarClaims(String evcsAccessToken) {
        this(evcsAccessToken, false);
    }

    public JarClaims(String evcsAccessToken, boolean updateIdentity) {
        this(
                updateIdentity ? Boolean.TRUE : null,
                new JarUserInfo(
                        new Essential(true),
                        new Essential(true),
                        new Essential(true),
                        null,
                        new Essential(true),
                        new Essential(true),
                        evcsAccessToken == null
                                ? null
                                : new ListOfStringValues(List.of(evcsAccessToken))));
    }
}
