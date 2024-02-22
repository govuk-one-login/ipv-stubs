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
import java.util.Optional;
import java.util.stream.Stream;

import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_CLAIM;

public class ContraIndicatorsService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_MESSAGE_DESCRIPTION = "description";
    private static final String LOG_ERROR_DESCRIPTION = "errorDescription";
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
            SignedJWT signedJWT = parseSignedJwt(request);
            String userId = signedJWT.getJWTClaimsSet().getSubject();
            var vcClaim = parseVcClaim(signedJWT);
            if (vcClaim != null) {
                var ci = vcClaim.evidence().get(0).ci();
                if (ci == null || ci.isEmpty()) {
                    LOGGER.info(
                            new StringMapMessage().with(LOG_MESSAGE_DESCRIPTION, "No CI in VC"));
                } else {
                    List<CimitStubItem> cimitStubItems =
                            mapToContraIndications(userId, vcClaim, signedJWT);
                    saveOrUpdateCimitStubItems(userId, cimitStubItems);
                }
            }
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

    private List<CimitStubItem> mapToContraIndications(
            String userId, VcClaim vcClaim, SignedJWT signedJWT) throws ParseException {

        Instant issuanceDate = getIssuanceDate(signedJWT.getJWTClaimsSet().getNotBeforeTime());
        String iss = signedJWT.getJWTClaimsSet().getIssuer();
        List<String> docId = getDocumentIdentifier(vcClaim);

        return vcClaim.evidence().get(0).ci().stream()
                .distinct()
                .map(
                        ciCode ->
                                CimitStubItem.builder()
                                        .userId(userId)
                                        .contraIndicatorCode(ciCode.toUpperCase())
                                        .issuers(List.of(iss))
                                        .issuanceDate(issuanceDate)
                                        .mitigations(List.of())
                                        .document(docId)
                                        .build())
                .toList();
    }

    private Instant getIssuanceDate(Date nbf) {
        if (nbf != null) {
            return nbf.toInstant();
        }
        return Instant.now();
    }

    private List<String> getDocumentIdentifier(VcClaim vcClaim) {
        CredentialSubject credentialSubject = vcClaim.credentialSubject();
        if (credentialSubject.drivingPermit() != null
                && !credentialSubject.drivingPermit().isEmpty()) {
            return List.of(credentialSubject.drivingPermit().get(0).toIdentifier());
        }
        if (credentialSubject.passport() != null && !credentialSubject.passport().isEmpty()) {
            return List.of(credentialSubject.passport().get(0).toIdentifier());
        }
        return List.of();
    }

    private void saveOrUpdateCimitStubItems(String userId, List<CimitStubItem> cimitStubItems) {
        List<CimitStubItem> dbCimitStubItems = cimitStubItemService.getCIsForUserId(userId);
        cimitStubItems.forEach(
                cimitStubItem -> {
                    Optional<CimitStubItem> dbCimitStubItem =
                            filterDbItemsForCiCode(
                                    dbCimitStubItems, cimitStubItem.getContraIndicatorCode());
                    if (dbCimitStubItem.isEmpty()) {
                        cimitStubItemService.persistCimitStubItem(cimitStubItem);
                    } else {
                        CimitStubItem dbItem = dbCimitStubItem.get();
                        dbItem.setIssuanceDate(cimitStubItem.getIssuanceDate());
                        dbItem.setIssuers(
                                mergeStringLists(dbItem.getIssuers(), cimitStubItem.getIssuers()));
                        dbItem.setDocument(
                                mergeStringLists(
                                        dbItem.getDocument(), cimitStubItem.getDocument()));
                        cimitStubItemService.updateCimitStubItem(dbItem);
                    }
                });
        LOGGER.info("Inserted User CI data to the Cimit Stub DynamoDB Table.");
    }

    private List<String> mergeStringLists(List<String> listOne, List<String> listTwo) {
        Stream<String> combinedStream = Stream.empty();
        if (listOne != null) {
            combinedStream = Stream.concat(combinedStream, listOne.stream());
        }
        if (listTwo != null) {
            combinedStream = Stream.concat(combinedStream, listTwo.stream());
        }
        return combinedStream.distinct().toList();
    }

    private Optional<CimitStubItem> filterDbItemsForCiCode(
            List<CimitStubItem> dbCimitStubItems, String code) {
        return dbCimitStubItems.stream()
                .filter(dbCimitStubItem -> dbCimitStubItem.getContraIndicatorCode().equals(code))
                .findFirst();
    }
}
