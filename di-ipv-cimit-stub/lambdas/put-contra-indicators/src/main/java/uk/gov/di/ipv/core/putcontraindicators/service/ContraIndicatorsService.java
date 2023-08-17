package uk.gov.di.ipv.core.putcontraindicators.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequest;
import uk.gov.di.ipv.core.putcontraindicators.dto.ContraIndicatorEvidenceDto;
import uk.gov.di.ipv.core.putcontraindicators.exceptions.CiPutException;

import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_EVIDENCE;

public class ContraIndicatorsService {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Gson gson = new Gson();

    private static final String LOG_MESSAGE_DESCRIPTION = "description";

    private static final String LOG_ERROR_DESCRIPTION = "errorDescription";

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
            ContraIndicatorEvidenceDto contraIndicatorEvidenceDto =
                    parseContraIndicatorEvidence(signedJWT);
            List<CimitStubItem> cimitStubItems =
                    mapToContraIndications(
                            userId,
                            contraIndicatorEvidenceDto,
                            getIssuanceDate(signedJWT.getJWTClaimsSet().getNotBeforeTime()));
            saveOrUpdateCimitStubItems(userId, cimitStubItems);
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

    private ContraIndicatorEvidenceDto parseContraIndicatorEvidence(SignedJWT signedJWT)
            throws CiPutException {
        JSONObject vcClaim;
        try {
            vcClaim = (JSONObject) signedJWT.getJWTClaimsSet().getClaim(VC_CLAIM);
        } catch (ParseException e) {
            String message = "Failed to parse VC claim";
            LOGGER.error(
                    new StringMapMessage()
                            .with(LOG_MESSAGE_DESCRIPTION, message)
                            .with(LOG_ERROR_DESCRIPTION, e.getMessage()));
            throw new CiPutException(message);
        }

        JSONArray evidenceArray = (JSONArray) vcClaim.get(VC_EVIDENCE);
        if (evidenceArray == null || evidenceArray.size() != 1) {
            String message = "Unexpected evidence count.";
            LOGGER.error(
                    new StringMapMessage()
                            .with(LOG_MESSAGE_DESCRIPTION, message)
                            .with(
                                    LOG_ERROR_DESCRIPTION,
                                    String.format(
                                            "Expected one evidence item, got %d.",
                                            evidenceArray == null ? 0 : evidenceArray.size())));
            throw new CiPutException(message);
        }

        List<ContraIndicatorEvidenceDto> contraIndicatorEvidenceDtos =
                gson.fromJson(
                        evidenceArray.toJSONString(),
                        new TypeToken<List<ContraIndicatorEvidenceDto>>() {}.getType());
        return contraIndicatorEvidenceDtos.get(0);
    }

    private List<CimitStubItem> mapToContraIndications(
            String userId,
            ContraIndicatorEvidenceDto contraIndicatorEvidenceDto,
            Instant issuanceDate) {
        if (contraIndicatorEvidenceDto.getCi().isEmpty()) {
            String message = "CI cannot be empty.";
            LOGGER.error(new StringMapMessage().with(LOG_MESSAGE_DESCRIPTION, message));
            throw new CiPutException(message);
        }

        return contraIndicatorEvidenceDto.getCi().stream()
                .distinct()
                .map(
                        ciCode -> {
                            return CimitStubItem.builder()
                                    .userId(userId)
                                    .contraIndicatorCode(ciCode)
                                    .issuanceDate(issuanceDate)
                                    .build();
                        })
                .collect(Collectors.toList());
    }

    private Instant getIssuanceDate(Date nbf) {
        if (nbf != null) {
            return nbf.toInstant();
        }
        return Instant.now();
    }

    private void saveOrUpdateCimitStubItems(String userId, List<CimitStubItem> cimitStubItems) {
        List<CimitStubItem> dbCimitStubItems = cimitStubItemService.getCIsForUserId(userId);
        cimitStubItems.forEach(
                cimitStubItem -> {
                    Optional<CimitStubItem> dbCimitStubItem =
                            getUserIdAndCodeFromDatabase(
                                    dbCimitStubItems, cimitStubItem.getContraIndicatorCode());
                    if (dbCimitStubItem.isEmpty()) {
                        cimitStubItemService.persistCimitStub(
                                userId,
                                cimitStubItem.getContraIndicatorCode().toUpperCase(),
                                cimitStubItem.getIssuanceDate(),
                                Collections.emptyList());
                    } else {
                        dbCimitStubItem.get().setIssuanceDate(cimitStubItem.getIssuanceDate());
                        cimitStubItemService.updateCimitStub(dbCimitStubItem.get());
                    }
                });
        LOGGER.info("Inserted User CI data to the Cimit Stub DynamoDB Table.");
    }

    private Optional<CimitStubItem> getUserIdAndCodeFromDatabase(
            List<CimitStubItem> cimitStubItems, String code) {
        return cimitStubItems.stream()
                .filter(cimitStubItem -> cimitStubItem.getContraIndicatorCode().equals(code))
                .findAny();
    }

}
