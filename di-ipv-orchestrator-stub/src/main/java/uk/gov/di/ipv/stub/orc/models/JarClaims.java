package uk.gov.di.ipv.stub.orc.models;

import java.util.List;

public record JarClaims(JarUserInfo userInfo) {
    public JarClaims(String inheritedIdentityJwt) {
        this(
                new JarUserInfo(
                        new Essential(true),
                        null,
                        null,
                        null,
                        inheritedIdentityJwt == null
                                ? null
                                : new InheritedIdentityJwtClaim(List.of(inheritedIdentityJwt))));
    }
}
