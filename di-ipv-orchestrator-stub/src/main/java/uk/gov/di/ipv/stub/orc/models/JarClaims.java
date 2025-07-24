package uk.gov.di.ipv.stub.orc.models;

import java.util.List;

public record JarClaims(JarUserInfo userinfo) {
    public JarClaims(String evcsAccessToken) {
        this(
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
