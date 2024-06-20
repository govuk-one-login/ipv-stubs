package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialRequest;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialResponse;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.ContraIndicator;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.MitigatingCredential;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.Mitigation;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.VcClaim;
import uk.gov.di.ipv.core.getcontraindicatorcredential.factory.ECDSASignerFactory;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_CLAIM;

@ExtendWith(MockitoExtension.class)
class GetContraIndicatorCredentialHandlerTest {
    private static final Instant NOW = Instant.now();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_ID = "user_id";
    private static final GetCiCredentialRequest GET_CI_CREDENTIAL_REQUEST =
            GetCiCredentialRequest.builder()
                    .govukSigninJourneyId("govuk_signin_journey_id")
                    .ipAddress("ip_address")
                    .userId(USER_ID)
                    .build();
    private static final String CI_V03 = "V03";
    private static final String CI_D02 = "D02";
    private static final String MITIGATION_M01 = "M01";
    private static final String CIMIT_PRIVATE_KEY =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO";
    private static final String CIMIT_PUBLIC_JWK =
            "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}";
    private static final String CIMIT_COMPONENT_ID = "https://cimit.stubs.account.gov.uk";
    private static final String ISSUER_1 = "issuer1";
    private static final String ISSUER_2 = "issuer2";
    private static final String ISSUER_3 = "issuer3";
    private static final String ISSUER_4 = "issuer4";
    private static final String DOC_1 = "doc1";
    private static final String DOC_2 = "doc2";
    private static final String TXN_1 = "1";
    private static final String TXN_2 = "2";
    private static final String TXN_3 = "3";

    @Mock private Context mockContext;
    @Mock private ConfigService mockConfigService;
    @Mock private CimitStubItemService mockCimitStubItemService;
    @Spy private ECDSASignerFactory spyEcdsaSignerFactory = new ECDSASignerFactory();
    @InjectMocks private GetContraIndicatorCredentialHandler getContraIndicatorCredentialHandler;

    @BeforeEach
    void setUp() {
        when(mockConfigService.getCimitSigningKey()).thenReturn(CIMIT_PRIVATE_KEY);
        when(mockConfigService.getCimitComponentId()).thenReturn(CIMIT_COMPONENT_ID);
    }

    @Test
    void shouldReturnSignedJwtWhenProvidedValidRequest() throws Exception {
        var cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_V03)
                                .issuer(ISSUER_1)
                                .issuanceDate(NOW)
                                .mitigations(List.of(MITIGATION_M01))
                                .txn(TXN_1)
                                .build());
        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response = makeRequest();

        verify(mockConfigService).getCimitSigningKey();
        verify(mockConfigService).getCimitComponentId();

        var signedJWT = SignedJWT.parse(response.getVc());
        var claimsSet = signedJWT.getJWTClaimsSet();

        assertEquals(USER_ID, signedJWT.getJWTClaimsSet().getClaim(JWTClaimNames.SUBJECT));
        assertEquals(
                CIMIT_COMPONENT_ID, signedJWT.getJWTClaimsSet().getClaim(JWTClaimNames.ISSUER));

        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);

        assertEquals(List.of("VerifiableCredential", "SecurityCheckCredential"), vcClaim.type());

        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();
        assertEquals(1, contraIndicators.size());

        var firstContraIndicator = contraIndicators.get(0);
        assertEquals(List.of(TXN_1), firstContraIndicator.getTxn());

        ContraIndicator expectedCi =
                ContraIndicator.builder()
                        .code(CI_V03)
                        .issuers(new TreeSet<>(List.of(ISSUER_1)))
                        .issuanceDate(NOW.toString())
                        .mitigation(
                                List.of(
                                        new Mitigation(
                                                MITIGATION_M01,
                                                List.of(MitigatingCredential.EMPTY))))
                        .incompleteMitigation(List.of())
                        .document(null)
                        .txn(List.of(TXN_1))
                        .build();
        assertEquals(expectedCi, firstContraIndicator);

        ECDSAVerifier verifier = new ECDSAVerifier(ECKey.parse(CIMIT_PUBLIC_JWK));
        assertTrue(signedJWT.verify(verifier));
    }

    @Test
    void singleUnmitigatedD02() throws Exception {
        var cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_1)
                                .issuanceDate(NOW)
                                .mitigations(List.of())
                                .document(DOC_1)
                                .txn(TXN_1)
                                .build());

        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response = makeRequest();

        var claimsSet = SignedJWT.parse(response.getVc()).getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);
        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();

        var expectedCi =
                List.of(
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_1)))
                                .issuanceDate(NOW.toString())
                                .mitigation(List.of())
                                .incompleteMitigation(List.of())
                                .document(DOC_1)
                                .txn(List.of(TXN_1))
                                .build());

        assertEquals(expectedCi, contraIndicators);
    }

    @Test
    void singleMitigatedD02() throws Exception {
        var cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_1)
                                .issuanceDate(NOW)
                                .mitigations(List.of(MITIGATION_M01))
                                .document(DOC_1)
                                .txn(TXN_1)
                                .build());

        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response = makeRequest();

        var claimsSet = SignedJWT.parse(response.getVc()).getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);
        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();

        var expectedCi =
                List.of(
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_1)))
                                .issuanceDate(NOW.toString())
                                .mitigation(
                                        List.of(
                                                new Mitigation(
                                                        MITIGATION_M01,
                                                        List.of(MitigatingCredential.EMPTY))))
                                .incompleteMitigation(List.of())
                                .document(DOC_1)
                                .txn(List.of(TXN_1))
                                .build());

        assertEquals(expectedCi, contraIndicators);
    }

    @Test
    void twoUnmitigatedD02ForDifferentDocuments() throws Exception {
        var cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_1)
                                .issuanceDate(NOW.minusSeconds(100L))
                                .mitigations(List.of())
                                .document(DOC_1)
                                .txn(TXN_1)
                                .build(),
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_2)
                                .issuanceDate(NOW)
                                .mitigations(List.of())
                                .document(DOC_2)
                                .txn(TXN_2)
                                .build());

        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response = makeRequest();

        var claimsSet = SignedJWT.parse(response.getVc()).getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);
        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();

        var expectedCi =
                List.of(
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_1)))
                                .issuanceDate(NOW.minusSeconds(100L).toString())
                                .mitigation(List.of())
                                .incompleteMitigation(List.of())
                                .document(DOC_1)
                                .txn(List.of(TXN_1))
                                .build(),
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_2)))
                                .issuanceDate(NOW.toString())
                                .mitigation(List.of())
                                .incompleteMitigation(List.of())
                                .document(DOC_2)
                                .txn(List.of(TXN_2))
                                .build());

        assertEquals(expectedCi, contraIndicators);
    }

    @Test
    void twoD02ForDifferentDocumentsWithOneMitigated() throws Exception {
        var cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_1)
                                .issuanceDate(NOW.minusSeconds(100L))
                                .mitigations(List.of())
                                .document(DOC_1)
                                .txn(TXN_1)
                                .build(),
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_2)
                                .issuanceDate(NOW)
                                .mitigations(List.of(MITIGATION_M01))
                                .document(DOC_2)
                                .txn(TXN_2)
                                .build());

        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response = makeRequest();

        var claimsSet = SignedJWT.parse(response.getVc()).getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);
        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();

        var expectedCi =
                List.of(
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_1)))
                                .issuanceDate(NOW.minusSeconds(100L).toString())
                                .mitigation(List.of())
                                .incompleteMitigation(List.of())
                                .document(DOC_1)
                                .txn(List.of(TXN_1))
                                .build(),
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_2)))
                                .issuanceDate(NOW.toString())
                                .mitigation(
                                        List.of(
                                                new Mitigation(
                                                        MITIGATION_M01,
                                                        List.of(MitigatingCredential.EMPTY))))
                                .incompleteMitigation(List.of())
                                .document(DOC_2)
                                .txn(List.of(TXN_2))
                                .build());

        assertEquals(expectedCi, contraIndicators);
    }

    @Test
    void twoD02ForDifferentDocumentsWithBothMitigated() throws Exception {
        var cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_1)
                                .issuanceDate(NOW.minusSeconds(100L))
                                .mitigations(List.of(MITIGATION_M01))
                                .document(DOC_1)
                                .txn(TXN_1)
                                .build(),
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_2)
                                .issuanceDate(NOW)
                                .mitigations(List.of(MITIGATION_M01))
                                .document(DOC_2)
                                .txn(TXN_2)
                                .build());

        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response = makeRequest();

        var claimsSet = SignedJWT.parse(response.getVc()).getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);
        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();

        var expectedCi =
                List.of(
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_1)))
                                .issuanceDate(NOW.minusSeconds(100L).toString())
                                .mitigation(
                                        List.of(
                                                new Mitigation(
                                                        MITIGATION_M01,
                                                        List.of(MitigatingCredential.EMPTY))))
                                .incompleteMitigation(List.of())
                                .document(DOC_1)
                                .txn(List.of(TXN_1))
                                .build(),
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_2)))
                                .issuanceDate(NOW.toString())
                                .mitigation(
                                        List.of(
                                                new Mitigation(
                                                        MITIGATION_M01,
                                                        List.of(MitigatingCredential.EMPTY))))
                                .incompleteMitigation(List.of())
                                .document(DOC_2)
                                .txn(List.of(TXN_2))
                                .build());

        assertEquals(expectedCi, contraIndicators);
    }

    @Test
    void twoD02ForSameDocumentWithNeitherMitigated() throws Exception {
        var cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_1)
                                .issuanceDate(NOW.minusSeconds(100L))
                                .mitigations(List.of())
                                .document(DOC_1)
                                .txn(TXN_1)
                                .build(),
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_2)
                                .issuanceDate(NOW)
                                .mitigations(List.of())
                                .document(DOC_1)
                                .txn(TXN_2)
                                .build());

        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response = makeRequest();

        var claimsSet = SignedJWT.parse(response.getVc()).getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);
        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();

        var expectedCi =
                List.of(
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_1, ISSUER_2)))
                                .issuanceDate(NOW.toString())
                                .mitigation(List.of())
                                .incompleteMitigation(List.of())
                                .document(DOC_1)
                                .txn(List.of(TXN_2))
                                .build());

        assertEquals(expectedCi, contraIndicators);
    }

    @Test
    void twoD02ForSameDocumentWithEarlierMitigated() throws Exception {
        var cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_1)
                                .issuanceDate(NOW.minusSeconds(100L))
                                .mitigations(List.of(MITIGATION_M01))
                                .document(DOC_1)
                                .txn(TXN_1)
                                .build(),
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_2)
                                .issuanceDate(NOW)
                                .mitigations(List.of())
                                .document(DOC_1)
                                .txn(TXN_2)
                                .build());

        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response = makeRequest();

        var claimsSet = SignedJWT.parse(response.getVc()).getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);
        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();

        var expectedCi =
                List.of(
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_1, ISSUER_2)))
                                .issuanceDate(NOW.toString())
                                .mitigation(List.of())
                                .incompleteMitigation(List.of())
                                .document(DOC_1)
                                .txn(List.of(TXN_2))
                                .build());

        assertEquals(expectedCi, contraIndicators);
    }

    @Test
    void twoD02ForSameDocumentWithBothMitigated() throws Exception {
        var cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_1)
                                .issuanceDate(NOW.minusSeconds(100L))
                                .mitigations(List.of(MITIGATION_M01))
                                .document(DOC_1)
                                .txn(TXN_1)
                                .build(),
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_2)
                                .issuanceDate(NOW)
                                .mitigations(List.of(MITIGATION_M01))
                                .document(DOC_1)
                                .txn(TXN_2)
                                .build());

        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response = makeRequest();

        var claimsSet = SignedJWT.parse(response.getVc()).getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);
        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();

        var expectedCi =
                List.of(
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_1, ISSUER_2)))
                                .issuanceDate(NOW.toString())
                                .mitigation(
                                        List.of(
                                                new Mitigation(
                                                        MITIGATION_M01,
                                                        List.of(MitigatingCredential.EMPTY))))
                                .incompleteMitigation(List.of())
                                .document(DOC_1)
                                .txn(List.of(TXN_2))
                                .build());

        assertEquals(expectedCi, contraIndicators);
    }

    @Test
    void twoD02ForDifferentDocumentsAndTwoMatchingNonDocCi() throws Exception {
        var cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_1)
                                .issuanceDate(NOW.minusSeconds(100L))
                                .mitigations(List.of())
                                .document(DOC_1)
                                .txn(TXN_1)
                                .build(),
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_D02)
                                .issuer(ISSUER_2)
                                .issuanceDate(NOW)
                                .mitigations(List.of())
                                .document(DOC_2)
                                .txn(TXN_2)
                                .build(),
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_V03)
                                .issuer(ISSUER_3)
                                .issuanceDate(NOW.minusSeconds(50L))
                                .mitigations(List.of())
                                .document(null)
                                .txn(TXN_3)
                                .build(),
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(CI_V03)
                                .issuer(ISSUER_4)
                                .issuanceDate(NOW)
                                .mitigations(List.of())
                                .document(null)
                                .txn(TXN_3)
                                .build());

        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response = makeRequest();

        var claimsSet = SignedJWT.parse(response.getVc()).getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);
        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();

        var expectedCi =
                List.of(
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_1)))
                                .issuanceDate(NOW.minusSeconds(100L).toString())
                                .mitigation(List.of())
                                .incompleteMitigation(List.of())
                                .document(DOC_1)
                                .txn(List.of(TXN_1))
                                .build(),
                        ContraIndicator.builder()
                                .code(CI_V03)
                                .issuers(new TreeSet<>(List.of(ISSUER_3, ISSUER_4)))
                                .issuanceDate(NOW.toString())
                                .mitigation(List.of())
                                .incompleteMitigation(List.of())
                                .document(null)
                                .txn(List.of(TXN_3))
                                .build(),
                        ContraIndicator.builder()
                                .code(CI_D02)
                                .issuers(new TreeSet<>(List.of(ISSUER_2)))
                                .issuanceDate(NOW.toString())
                                .mitigation(List.of())
                                .incompleteMitigation(List.of())
                                .document(DOC_2)
                                .txn(List.of(TXN_2))
                                .build());

        assertEquals(expectedCi, contraIndicators);
    }

    @Test
    void shouldFailForWrongCimitKey() throws IOException {
        when(mockConfigService.getCimitSigningKey()).thenReturn("Invalid_cimit_key");

        var response = makeRequest();

        verify(mockConfigService).getCimitSigningKey();

        assertEquals("Failure", response.getVc());
    }

    private GetCiCredentialResponse makeRequest() throws IOException {
        try (var inputStream =
                        new ByteArrayInputStream(
                                objectMapper.writeValueAsBytes(GET_CI_CREDENTIAL_REQUEST));
                var outputStream = new ByteArrayOutputStream()) {
            getContraIndicatorCredentialHandler.handleRequest(
                    inputStream, outputStream, mockContext);
            return objectMapper.readValue(outputStream.toString(), GetCiCredentialResponse.class);
        }
    }
}
