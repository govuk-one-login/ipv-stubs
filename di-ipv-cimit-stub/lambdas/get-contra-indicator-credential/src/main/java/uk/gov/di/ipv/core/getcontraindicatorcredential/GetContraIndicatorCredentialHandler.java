package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialRequest;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialResponse;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.ContraIndicator;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.Evidence;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.MitigatingCredential;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.Mitigation;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.VcClaim;
import uk.gov.di.ipv.core.getcontraindicatorcredential.factory.ECDSASignerFactory;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import static java.util.Comparator.comparing;
import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_CLAIM;

public class GetContraIndicatorCredentialHandler implements RequestStreamHandler {

    private static final String FAILURE_RESPONSE = "Failure";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JWSHeader JWT_HEADER =
            new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
    private final ConfigService configService;
    private final CimitStubItemService cimitStubItemService;
    private final ECDSASignerFactory signerFactory;

    public GetContraIndicatorCredentialHandler() {
        this.configService = new ConfigService();
        this.cimitStubItemService = new CimitStubItemService(configService);
        this.signerFactory = new ECDSASignerFactory();
    }

    public GetContraIndicatorCredentialHandler(
            ConfigService configService,
            CimitStubItemService cimitStubItemService,
            ECDSASignerFactory signerFactory) {
        this.configService = configService;
        this.cimitStubItemService = cimitStubItemService;
        this.signerFactory = signerFactory;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        LOGGER.info(
                new StringMapMessage().with("Function invoked:", "GetContraIndicatorCredential"));
        GetCiCredentialRequest event = null;
        String response = null;
        try {
            event = OBJECT_MAPPER.readValue(input, GetCiCredentialRequest.class);
        } catch (Exception e) {
            LOGGER.error(
                    new StringMapMessage().with("Unable to parse input request", e.getMessage()));
            response = FAILURE_RESPONSE;
        }

        if (response == null) {
            SignedJWT signedJWT;
            try {
                signedJWT = generateJWT(event.getUserId());
                response = signedJWT.serialize();
            } catch (Exception ex) {
                LOGGER.error(
                        new StringMapMessage()
                                .with(
                                        "errorDescription",
                                        "Failed at stub during creation of signedJwt. Error message:"
                                                + ex.getMessage()));
                response = FAILURE_RESPONSE;
            }
        }
        OBJECT_MAPPER.writeValue(output, new GetCiCredentialResponse(response));
    }

    private SignedJWT generateJWT(String userId)
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {
        SignedJWT signedJWT = new SignedJWT(JWT_HEADER, generateClaimsSet(userId));
        signedJWT.sign(signerFactory.getSigner(configService.getCimitSigningKey()));

        return signedJWT;
    }

    private JWTClaimsSet generateClaimsSet(String userId) {
        var now = Instant.now();
        return new JWTClaimsSet.Builder()
                .claim(JWTClaimNames.SUBJECT, userId)
                .claim(JWTClaimNames.ISSUER, configService.getCimitComponentId())
                .claim(JWTClaimNames.NOT_BEFORE, now.getEpochSecond())
                .claim(JWTClaimNames.EXPIRATION_TIME, now.plusSeconds(60L * 15L).getEpochSecond())
                .claim(
                        VC_CLAIM,
                        OBJECT_MAPPER.convertValue(
                                generateVc(userId), new TypeReference<Map<String, Object>>() {}))
                .build();
    }

    private VcClaim generateVc(String userId) {
        var txnUuid = List.of(UUID.randomUUID().toString());
        var contraIndicators = getContraIndicators(userId, txnUuid);
        return new VcClaim(
                List.of(
                        new Evidence(
                                contraIndicators,
                                contraIndicators.isEmpty() ? List.of() : txnUuid)));
    }

    private List<ContraIndicator> getContraIndicators(String userId, List<String> txnUuid) {
        var ciFromDatabase =
                cimitStubItemService.getCIsForUserId(userId).stream()
                        .sorted(comparing(CimitStubItem::getIssuanceDate))
                        .toList();

        var ciForVc = new ArrayList<ContraIndicator>();
        for (var datebaseCi : ciFromDatabase) {
            ciForVc.stream()
                    .filter(vcCi -> ciShouldBeMerged(datebaseCi, vcCi))
                    .findFirst()
                    .ifPresentOrElse(
                            vcCi -> {
                                vcCi.getIssuers().add(datebaseCi.getIssuer());
                                vcCi.setIssuanceDate(datebaseCi.getIssuanceDate().toString());
                                vcCi.setMitigation(getMitigations(datebaseCi.getMitigations()));
                            },
                            () -> ciForVc.add(createContraIndicator(datebaseCi, txnUuid)));
        }
        return ciForVc;
    }

    private boolean ciShouldBeMerged(
            CimitStubItem ciBeingProcessed, ContraIndicator ciAlreadyProcessed) {
        boolean ciCodesMatch =
                ciBeingProcessed.getContraIndicatorCode().equals(ciAlreadyProcessed.getCode());
        boolean documentsMatch =
                ciBeingProcessed.getDocument() == null
                        ? ciAlreadyProcessed.getDocument() == null
                        : ciBeingProcessed.getDocument().equals(ciAlreadyProcessed.getDocument());

        return ciCodesMatch && documentsMatch;
    }

    private ContraIndicator createContraIndicator(CimitStubItem item, List<String> txnUuid) {
        return new ContraIndicator(
                item.getContraIndicatorCode(),
                item.getDocument(),
                item.getIssuanceDate().toString(),
                new TreeSet<>(List.of(item.getIssuer())),
                getMitigations(item.getMitigations()),
                List.of(),
                txnUuid);
    }

    private List<Mitigation> getMitigations(List<String> mitigationCodes) {
        return mitigationCodes.stream()
                .map(ciCode -> new Mitigation(ciCode, List.of(MitigatingCredential.EMPTY)))
                .toList();
    }
}
