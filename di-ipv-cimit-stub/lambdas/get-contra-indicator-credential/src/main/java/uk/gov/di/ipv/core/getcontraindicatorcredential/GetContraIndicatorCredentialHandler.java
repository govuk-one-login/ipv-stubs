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
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialErrorResponse;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import static java.util.Comparator.comparing;
import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_CLAIM;

public class GetContraIndicatorCredentialHandler implements RequestStreamHandler {

    private static final String INTERNAL_ERROR_TYPE = "INTERNAL_ERROR";
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
        GetCiCredentialResponse response = null;
        try {
            event = OBJECT_MAPPER.readValue(input, GetCiCredentialRequest.class);
        } catch (Exception e) {
            LOGGER.error(
                    new StringMapMessage().with("Unable to parse input request", e.getMessage()));
            var errorResponse =
                    new GetCiCredentialErrorResponse(
                            INTERNAL_ERROR_TYPE,
                            "Unable to parse input request. Error message: " + e.getMessage());
            OBJECT_MAPPER.writeValue(output, errorResponse);
            return;
        }

        try {
            SignedJWT signedJWT = generateJWT(event.getUserId());
            response = new GetCiCredentialResponse(signedJWT.serialize());
        } catch (Exception ex) {
            LOGGER.error(
                    new StringMapMessage()
                            .with(
                                    "errorDescription",
                                    "Failed at stub during creation of signedJwt. Error message:"
                                            + ex.getMessage()));

            // It is possible to catch an exception here with a null message. Log the stack trace so
            // that we can see what is going on.
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            ex.printStackTrace(printWriter);
            printWriter.flush();

            LOGGER.error(
                    new StringMapMessage()
                            .with(
                                    "errorDescription",
                                    "Failed at stub during creation of signedJwt. Error trace:"
                                            + writer));

            var errorResponse =
                    new GetCiCredentialErrorResponse(
                            INTERNAL_ERROR_TYPE,
                            "Failed at stub during creation of signedJwt. Error message: "
                                    + ex.getMessage());
            OBJECT_MAPPER.writeValue(output, errorResponse);
            return;
        }

        OBJECT_MAPPER.writeValue(output, response);
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
        var contraIndicators = getContraIndicators(userId);
        return new VcClaim(List.of(new Evidence(contraIndicators)));
    }

    private List<ContraIndicator> getContraIndicators(String userId) {
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
                                vcCi.setTxn(List.of(datebaseCi.getTxn()));
                            },
                            () -> ciForVc.add(createContraIndicator(datebaseCi)));
        }
        return ciForVc;
    }

    private boolean ciShouldBeMerged(
            CimitStubItem ciBeingProcessed, ContraIndicator ciAlreadyProcessed) {
        boolean ciCodesMatch =
                ciBeingProcessed.getContraIndicatorCode().equals(ciAlreadyProcessed.getCode());
        boolean documentsMatch =
                Objects.equals(ciAlreadyProcessed.getDocument(), ciBeingProcessed.getDocument());

        return ciCodesMatch && documentsMatch;
    }

    private ContraIndicator createContraIndicator(CimitStubItem item) {
        if (item.getIssuer() == null) {
            throw new InvalidParameterException("Stub item has null issuer");
        }
        if (item.getTxn() == null) {
            throw new InvalidParameterException("Stub item has null txn");
        }
        return new ContraIndicator(
                item.getContraIndicatorCode(),
                item.getDocument(),
                item.getIssuanceDate().toString(),
                new TreeSet<>(List.of(item.getIssuer())),
                getMitigations(item.getMitigations()),
                List.of(),
                List.of(item.getTxn()));
    }

    private List<Mitigation> getMitigations(List<String> mitigationCodes) {
        if (mitigationCodes == null) {
            LOGGER.warn("Mitigations on CimitStubItem are null");
            return List.of();
        }
        return mitigationCodes.stream()
                .map(ciCode -> new Mitigation(ciCode, List.of(MitigatingCredential.EMPTY)))
                .toList();
    }
}
