package uk.gov.di.ipv.stub.orc.models;

import java.util.List;

public record JarClaims(JarUserInfo userInfo) {
    public JarClaims(String inheritedIdentityJwt, String evcsAccessToken) {
        this(
                new JarUserInfo(
                        new Essential(true),
                        null,
                        null,
                        null,
                        inheritedIdentityJwt == null
                                ? null
                                : new ListOfStringValues(List.of(inheritedIdentityJwt)),
                        evcsAccessToken == null
                                ? null
                                : new ListOfStringValues(List.of(evcsAccessToken))));
    }
}
