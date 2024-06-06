package uk.gov.di.ipv.stub.orc.models;

import java.util.List;

public record JarClaims(JarUserInfo userinfo) {
    public JarClaims(String inheritedIdentityJwt, String evcsAccessToken) {
        this(
                new JarUserInfo(
                        new Essential(true),
                        new Essential(true),
                        new Essential(true),
                        null,
                        inheritedIdentityJwt == null
                                ? null
                                : new ListOfStringValues(List.of(inheritedIdentityJwt)),
                        evcsAccessToken == null
                                ? null
                                : new ListOfStringValues(List.of(evcsAccessToken))));
    }
}
