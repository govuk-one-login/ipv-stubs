package uk.gov.di.ipv.core.putcontraindicators.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import software.amazon.awssdk.utils.StringUtils;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequest;
import uk.gov.di.ipv.core.putcontraindicators.dto.ContraIndicatorDto;
import uk.gov.di.ipv.core.putcontraindicators.dto.ContraIndicatorEvidenceDto;
import uk.gov.di.ipv.core.putcontraindicators.dto.MitigationDto;
import uk.gov.di.ipv.core.putcontraindicators.exceptions.CiPutException;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.di.ipv.core.putcontraindicators.domain.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.core.putcontraindicators.domain.VerifiableCredentialConstants.VC_EVIDENCE;

public class CimitService {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Gson gson = new Gson();

    private static final String LOG_MESSAGE_DESCRIPTION = "description";

    private static final String LOG_ERROR_DESCRIPTION = "errorDescription";

    private final ConfigService configService;

    private final CimitStubService cimitStubService;

    public CimitService() {
        this.configService = new ConfigService();
        this.cimitStubService = new CimitStubService(configService);
    }

    public CimitService(ConfigService configService, CimitStubService cimitStubService) {
        this.configService = configService;
        this.cimitStubService = cimitStubService;
    }

    public void addUserCis(PutContraIndicatorsRequest putContraIndicatorsRequest)
            throws CiPutException {
        try {
            SignedJWT signedJWT = getSignedJwt(putContraIndicatorsRequest);
            String userId = signedJWT.getJWTClaimsSet().getSubject();
            ContraIndicatorEvidenceDto contraIndicatorEvidenceDto =
                    getContraIndicatorsVC(signedJWT);
            List<CimitStubItem> cimitStubItems =
                    mapToContraIndications(userId, contraIndicatorEvidenceDto);
            addCimitStubItems(userId, cimitStubItems);
        } catch (Exception ex) {
            throw new CiPutException(ex.getMessage());
        }
    }

    private SignedJWT getSignedJwt(PutContraIndicatorsRequest putContraIndicatorsRequest)
            throws CiPutException {
        return getContraIndicatorJWT(putContraIndicatorsRequest.getSignedJwt());
    }

    private ContraIndicatorEvidenceDto getContraIndicatorsVC(SignedJWT ciSignedJWT) {
        return parseContraIndicatorEvidence(ciSignedJWT);
    }

    private List<CimitStubItem> mapToContraIndications(
            String userId, ContraIndicatorEvidenceDto contraIndicatorEvidenceDto) {
        if (contraIndicatorEvidenceDto.getCi().isEmpty()) {
            String message = "CI cannot be empty.";
            LOGGER.error(new StringMapMessage().with(LOG_MESSAGE_DESCRIPTION, message));
            throw new CiPutException(message);
        }

        Map<String, List<ContraIndicatorDto>> groupedByCICode =
                contraIndicatorEvidenceDto.getCi().stream()
                        .filter(ci -> ci != null && ci.getCode() != null)
                        .collect(Collectors.groupingBy(ContraIndicatorDto::getCode));

        return groupedByCICode.entrySet().stream()
                .map(
                        ciEntry -> {
                            String contraIndicatorCode = ciEntry.getKey();
                            List<ContraIndicatorDto> contraIndicatorList = ciEntry.getValue();
                            List<String> mitigationCodes =
                                    contraIndicatorList.stream()
                                            .flatMap(ci -> ci.getMitigation().stream())
                                            .map(MitigationDto::getCode)
                                            .collect(Collectors.toList());
                            Instant issuanceDate =
                                    getIssuanceDate(contraIndicatorList.get(0).getIssuanceDate());
                            return CimitStubItem.builder()
                                    .userId(userId)
                                    .contraIndicatorCode(contraIndicatorCode)
                                    .issuanceDate(issuanceDate)
                                    .mitigations(mitigationCodes)
                                    .build();
                        })
                .collect(Collectors.toList());
    }

    private void addCimitStubItems(String userId, List<CimitStubItem> cimitStubItems) {
        List<CimitStubItem> dbCimitStubItems = cimitStubService.getCimitStubItems(userId);
        cimitStubItems.forEach(
                cimitStubItem -> {
                    Optional<CimitStubItem> dbCimitStubItem =
                            getUserIdAndCodeFromDatabase(
                                    dbCimitStubItems, cimitStubItem.getContraIndicatorCode());
                    if (dbCimitStubItem.isEmpty()) {
                        cimitStubService.persistCimitStub(
                                userId,
                                cimitStubItem.getContraIndicatorCode(),
                                cimitStubItem.getIssuanceDate(),
                                cimitStubItem.getMitigations());
                    } else {
                        dbCimitStubItem
                                .get()
                                .setMitigations(
                                        getUpdatedMitigationsList(
                                                dbCimitStubItem.get().getMitigations(),
                                                cimitStubItem.getMitigations()));
                        dbCimitStubItem.get().setIssuanceDate(cimitStubItem.getIssuanceDate());
                        cimitStubService.updateCimitStub(dbCimitStubItem.get());
                    }
                });
        LOGGER.info("Inserted User CI data to the Cimit Stub DynamoDB Table.");
    }

    private List<String> getUpdatedMitigationsList(
            List<String> existingMitigations, List<String> newMitigations) {
        return Stream.concat(existingMitigations.stream(), newMitigations.stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private Optional<CimitStubItem> getUserIdAndCodeFromDatabase(
            List<CimitStubItem> cimitStubItems, String code) {
        return cimitStubItems.stream()
                .filter(cimitStubItem -> cimitStubItem.getContraIndicatorCode().equals(code))
                .findAny();
    }

    private SignedJWT getContraIndicatorJWT(String contraIndicatorVC) throws CiPutException {
        SignedJWT contraIndicatorsJwt;
        try {
            contraIndicatorsJwt = SignedJWT.parse(contraIndicatorVC);
        } catch (ParseException e) {
            LOGGER.error(
                    new StringMapMessage()
                            .with(
                                    LOG_ERROR_DESCRIPTION,
                                    "Failed to parse ContraIndicators JWT. Error message:"
                                            + e.getMessage()));
            throw new CiPutException("Failed to parse JWT");
        }
        return contraIndicatorsJwt;
    }

    private ContraIndicatorEvidenceDto parseContraIndicatorEvidence(SignedJWT signedJWT)
            throws CiPutException {
        JSONObject vcClaim;
        try {
            vcClaim = (JSONObject) signedJWT.getJWTClaimsSet().getClaim(VC_CLAIM);
        } catch (ParseException e) {
            String message = "Failed to parse ContraIndicators response json";
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

    private Instant getIssuanceDate(String issuanceDate) {
        if (!StringUtils.isEmpty(issuanceDate)) {
            return Instant.parse(issuanceDate);
        }
        return Instant.now();
    }
}
