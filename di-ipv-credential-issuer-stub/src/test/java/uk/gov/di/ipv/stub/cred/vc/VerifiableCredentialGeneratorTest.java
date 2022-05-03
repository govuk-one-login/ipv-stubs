package uk.gov.di.ipv.stub.cred.vc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.di.ipv.stub.cred.domain.Credential;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.EC_PRIVATE_KEY_1;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.EC_PUBLIC_KEY_1;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.CREDENTIAL_SUBJECT_ADDRESS;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.CREDENTIAL_SUBJECT_BIRTH_DATE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.CREDENTIAL_SUBJECT_NAME;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.DI_CONTEXT;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.IDENTITY_CHECK_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_CONTEXT;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_CREDENTIAL_SUBJECT;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_EVIDENCE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_TYPE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VERIFIABLE_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.W3_BASE_CONTEXT;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialGenerator.EC_ALGO;

@ExtendWith(SystemStubsExtension.class)
public class VerifiableCredentialGeneratorTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final VerifiableCredentialGenerator vcGenerator =
            new VerifiableCredentialGenerator();

    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables(
                    "VC_ISSUER", "https://issuer.example.com", "VC_SIGNING_KEY", EC_PRIVATE_KEY_1);

    @Test
    void shouldGenerateASignedVerifiableCredential() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(
                "name",
                List.of(
                        Map.of(
                                "nameParts",
                                List.of(Map.of("value", "Chris", "type", "GivenName")))));
        attributes.put("birthDate", List.of(Map.of("value", "1984-09-28")));
        attributes.put(
                "address", List.of(Map.of("type", "PostalAddress", "postalCode", "LE12 9BN")));

        Map<String, Object> evidence =
                Map.of(
                        "type", "CriStubCheck",
                        "strength", 4,
                        "validity", 2);
        String userId = "user-id";
        Credential credential = new Credential(attributes, evidence, userId);

        SignedJWT verifiableCredential = vcGenerator.generate(credential);

        KeyFactory kf = KeyFactory.getInstance(EC_ALGO);
        ECPublicKey ecPublicKey =
                (ECPublicKey)
                        kf.generatePublic(
                                new X509EncodedKeySpec(
                                        Base64.getDecoder().decode(EC_PUBLIC_KEY_1)));
        ECDSAVerifier ecVerifier = new ECDSAVerifier(ecPublicKey);
        assertTrue(verifiableCredential.verify(ecVerifier));

        JsonNode claimsSetTree =
                objectMapper.valueToTree(verifiableCredential.getJWTClaimsSet()).path("claims");

        assertEquals("https://issuer.example.com", claimsSetTree.path(ISSUER).asText());
        assertEquals(userId, claimsSetTree.path(SUBJECT).asText());
        assertNotNull(claimsSetTree.path(NOT_BEFORE));
        assertEquals(
                300,
                claimsSetTree.path(EXPIRATION_TIME).asLong()
                        - claimsSetTree.path(NOT_BEFORE).asLong());

        JsonNode vcClaimTree = claimsSetTree.path(VC_CLAIM);
        assertEquals(W3_BASE_CONTEXT, vcClaimTree.path(VC_CONTEXT).path(0).asText());
        assertEquals(DI_CONTEXT, vcClaimTree.path(VC_CONTEXT).path(1).asText());

        assertEquals(VERIFIABLE_CREDENTIAL_TYPE, vcClaimTree.path(VC_TYPE).path(0).asText());
        assertEquals(IDENTITY_CHECK_CREDENTIAL_TYPE, vcClaimTree.path(VC_TYPE).path(1).asText());

        JsonNode credSubjectTree = vcClaimTree.path(VC_CREDENTIAL_SUBJECT);
        assertEquals(
                "Chris",
                credSubjectTree
                        .path(CREDENTIAL_SUBJECT_NAME)
                        .path(0)
                        .path("nameParts")
                        .path(0)
                        .path("value")
                        .asText());
        assertEquals(
                "GivenName",
                credSubjectTree
                        .path(CREDENTIAL_SUBJECT_NAME)
                        .path(0)
                        .path("nameParts")
                        .path(0)
                        .path("type")
                        .asText());
        assertEquals(
                "1984-09-28",
                credSubjectTree.path(CREDENTIAL_SUBJECT_BIRTH_DATE).path(0).path("value").asText());
        assertEquals(
                "PostalAddress",
                credSubjectTree.path(CREDENTIAL_SUBJECT_ADDRESS).path(0).path("type").asText());
        assertEquals(
                "LE12 9BN",
                credSubjectTree
                        .path(CREDENTIAL_SUBJECT_ADDRESS)
                        .path(0)
                        .path("postalCode")
                        .asText());

        JsonNode evidenceTree = vcClaimTree.path(VC_EVIDENCE);
        assertEquals("CriStubCheck", evidenceTree.path(0).path("type").asText());
        assertEquals(4, evidenceTree.path(0).path("strength").asInt());
        assertEquals(2, evidenceTree.path(0).path("validity").asInt());
    }

    @Test
    void shouldNotIncludeSharedAttributesThatAreNotPopulated() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", List.of());
        attributes.put("birthDate", List.of(Map.of("value", "1984-09-28")));

        Map<String, Object> evidence =
                Map.of(
                        "type", "CriStubCheck",
                        "strength", 4,
                        "validity", 2);
        String userId = "user-id";
        Credential credential = new Credential(attributes, evidence, userId);

        SignedJWT verifiableCredential = vcGenerator.generate(credential);

        JsonNode claimsSetTree =
                objectMapper.valueToTree(verifiableCredential.getJWTClaimsSet()).path("claims");

        assertNull(
                claimsSetTree
                        .get(VC_CLAIM)
                        .get(VC_CREDENTIAL_SUBJECT)
                        .get(CREDENTIAL_SUBJECT_NAME));
        assertNull(
                claimsSetTree
                        .get(VC_CLAIM)
                        .get(VC_CREDENTIAL_SUBJECT)
                        .get(CREDENTIAL_SUBJECT_ADDRESS));
    }
}
