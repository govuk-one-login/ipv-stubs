package uk.gov.di.ipv.stub.orc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import uk.gov.di.ipv.stub.orc.models.Essential;
import uk.gov.di.ipv.stub.orc.models.InheritedIdentityJwtClaim;
import uk.gov.di.ipv.stub.orc.models.JarClaims;
import uk.gov.di.ipv.stub.orc.models.JarUserInfo;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public class JarClaimsBuilder {
    public static JarClaims buildJarClaims(
            String userId,
            String vot,
            boolean duringMigration,
            String credentialSubject,
            String evidence) throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException, JsonProcessingException {
        var inheritedIdentityJwt = duringMigration
                ? InheritedIdentityJwtBuilder.generate(userId, vot, credentialSubject, evidence).serialize()
                : null;
        return new JarClaims(new JarUserInfo(
                new Essential(true),
                null,
                null,
                null,
                inheritedIdentityJwt == null
                        ? null
                        : new InheritedIdentityJwtClaim(List.of(inheritedIdentityJwt))
        ));
    }
}
