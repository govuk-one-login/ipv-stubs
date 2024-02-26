package uk.gov.di.ipv.core.putcontraindicators.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequest;
import uk.gov.di.ipv.core.putcontraindicators.dto.CredentialSubject;
import uk.gov.di.ipv.core.putcontraindicators.dto.Evidence;
import uk.gov.di.ipv.core.putcontraindicators.dto.VcClaim;
import uk.gov.di.ipv.core.putcontraindicators.exceptions.CiPutException;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_CLAIM;

public class ContraIndicatorsService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_MESSAGE_DESCRIPTION = "description";
    private static final String LOG_ERROR_DESCRIPTION = "errorDescription";
    private static final String LOG_ITEM_COUNT = "itemCount";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ConfigService configService;
    private final CimitStubItemService cimitStubItemService;

    public ContraIndicatorsService() {
        this.configService = new ConfigService();
        this.cimitStubItemService = new CimitStubItemService(configService);
    }

    public ContraIndicatorsService(
            ConfigService configService, CimitStubItemService cimitStubItemService) {
        this.configService = configService;
        this.cimitStubItemService = cimitStubItemService;
    }

    public void addUserCis(PutContraIndicatorsRequest request) throws CiPutException {
        try {
            var signedJWT = parseSignedJwt(request);
            var cimitStubItems = toCimitStubItems(signedJWT);
            cimitStubItems.forEach(cimitStubItemService::persistCimitStubItem);
            LOGGER.info(
                    new StringMapMessage()
                            .with(
                                    LOG_MESSAGE_DESCRIPTION,
                                    "Inserted User CI data to the Cimit Stub DynamoDB Table.")
                            .with(LOG_ITEM_COUNT, cimitStubItems.size()));
        } catch (Exception ex) {
            throw new CiPutException(ex.getMessage());
        }
    }

    private SignedJWT parseSignedJwt(PutContraIndicatorsRequest request) throws CiPutException {
        try {
            return SignedJWT.parse(request.getSignedJwt());
        } catch (ParseException e) {
            LOGGER.error(
                    new StringMapMessage()
                            .with(
                                    LOG_ERROR_DESCRIPTION,
                                    "Failed to parse ContraIndicators JWT. Error message:"
                                            + e.getMessage()));
            throw new CiPutException("Failed to parse JWT");
        }
    }

    private VcClaim parseVcClaim(SignedJWT signedJWT) throws CiPutException {
        VcClaim vcClaim;
        try {
            vcClaim =
                    OBJECT_MAPPER.convertValue(
                            signedJWT.getJWTClaimsSet().getClaim(VC_CLAIM), VcClaim.class);
        } catch (ParseException e) {
            LOGGER.info(
                    new StringMapMessage()
                            .with(LOG_MESSAGE_DESCRIPTION, "Failed to parse VC claim")
                            .with(LOG_ERROR_DESCRIPTION, e.getMessage()));
            return null;
        }

        List<Evidence> evidence = vcClaim.evidence();
        if (evidence == null || evidence.size() != 1) {
            String message = "Unexpected evidence count.";
            LOGGER.info(
                    new StringMapMessage()
                            .with(LOG_MESSAGE_DESCRIPTION, message)
                            .with(
                                    LOG_ERROR_DESCRIPTION,
                                    String.format(
                                            "Expected one evidence item, got %d.",
                                            evidence == null ? 0 : evidence.size())));
            return null;
        }

        return vcClaim;
    }

    private List<CimitStubItem> toCimitStubItems(SignedJWT signedJWT) throws ParseException {
        var vcClaim = parseVcClaim(signedJWT);
        if (vcClaim == null) {
            LOGGER.info(
                    new StringMapMessage()
                            .with(LOG_MESSAGE_DESCRIPTION, "No VC claim found in jwt"));
            return List.of();
        }

        var ci = vcClaim.evidence().get(0).ci();
        if (ci == null || ci.isEmpty()) {
            LOGGER.info(new StringMapMessage().with(LOG_MESSAGE_DESCRIPTION, "No CI in VC"));
            return List.of();
        }

        var userId = signedJWT.getJWTClaimsSet().getSubject();
        var iss = signedJWT.getJWTClaimsSet().getIssuer();
        var issuanceDate = getIssuanceDate(signedJWT.getJWTClaimsSet().getNotBeforeTime());
        var docId = getDocumentIdentifier(vcClaim);

        return ci.stream()
                .distinct()
                .map(
                        ciCode ->
                                CimitStubItem.builder()
                                        .userId(userId)
                                        .contraIndicatorCode(ciCode.toUpperCase())
                                        .issuer(iss)
                                        .issuanceDate(issuanceDate)
                                        .mitigations(List.of())
                                        .documentIdentifier(docId)
                                        .build())
                .toList();
    }

    private Instant getIssuanceDate(Date nbf) {
        if (nbf != null) {
            return nbf.toInstant();
        }
        return Instant.now();
    }

    private String getDocumentIdentifier(VcClaim vcClaim) {
        CredentialSubject credentialSubject = vcClaim.credentialSubject();
        if (credentialSubject.drivingPermit() != null
                && !credentialSubject.drivingPermit().isEmpty()) {
            return credentialSubject.drivingPermit().get(0).toIdentifier();
        }
        if (credentialSubject.passport() != null && !credentialSubject.passport().isEmpty()) {
            return credentialSubject.passport().get(0).toIdentifier();
        }
        return null;
    }
}
