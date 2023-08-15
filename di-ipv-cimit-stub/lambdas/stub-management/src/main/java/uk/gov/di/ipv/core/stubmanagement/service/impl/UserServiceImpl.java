package uk.gov.di.ipv.core.stubmanagement.service.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.utils.StringUtils;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.stubmanagement.exceptions.BadRequestException;
import uk.gov.di.ipv.core.stubmanagement.exceptions.DataNotFoundException;
import uk.gov.di.ipv.core.stubmanagement.model.UserCisRequest;
import uk.gov.di.ipv.core.stubmanagement.model.UserMitigationRequest;
import uk.gov.di.ipv.core.stubmanagement.service.UserService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ConfigService configService;
    private final CimitStubService cimitStubService;

    public UserServiceImpl() {
        this.configService = new ConfigService();
        this.cimitStubService = new CimitStubService(configService);
    }

    public UserServiceImpl(ConfigService configService, CimitStubService cimitStubService) {
        this.configService = configService;
        this.cimitStubService = cimitStubService;
    }

    @Override
    public void addUserCis(String userId, List<UserCisRequest> userCisRequest) {
        checkCICodes(userCisRequest);
        List<CimitStubItem> cimitStubItems = cimitStubService.getCimitStubItems(userId);
        userCisRequest.forEach(
                user -> {
                    Optional<CimitStubItem> cimitStubItem =
                            getUserIdAndCodeFromDatabase(cimitStubItems, user.getCode());
                    if (cimitStubItem.isEmpty()) {
                        cimitStubService.persistCimitStub(
                                userId,
                                user.getCode().toUpperCase(),
                                getIssuanceDate(user.getIssuanceDate()),
                                convertListToUppercase(user.getMitigations()));
                    } else {
                        cimitStubItem
                                .get()
                                .setMitigations(
                                        getUpdatedMitigationsList(
                                                cimitStubItem.get().getMitigations(),
                                                user.getMitigations()));
                        cimitStubItem
                                .get()
                                .setIssuanceDate(getIssuanceDate(user.getIssuanceDate()));
                        cimitStubService.updateCimitStub(cimitStubItem.get());
                    }
                });
        LOGGER.info("Inserted User CI data to the Cimit Stub DynamoDB Table.");
    }

    @Override
    public void updateUserCis(String userId, List<UserCisRequest> userCisRequest) {
        checkCICodes(userCisRequest);
        List<CimitStubItem> cimitStubItems = cimitStubService.getCimitStubItems(userId);
        if (!cimitStubItems.isEmpty()) {
            deleteCimitStubItems(cimitStubItems);
        }
        userCisRequest.forEach(
                user -> {
                    cimitStubService.persistCimitStub(
                            userId,
                            user.getCode().toUpperCase(),
                            getIssuanceDate(user.getIssuanceDate()),
                            convertListToUppercase(user.getMitigations()));
                });
    }

    private void deleteCimitStubItems(List<CimitStubItem> cimitStubItems) {
        cimitStubItems.stream()
                .forEach(
                        item ->
                                cimitStubService.deleteCimitStubItem(
                                        item.getUserId(), item.getContraIndicatorCode()));
    }

    private static void checkCICodes(List<UserCisRequest> userCisRequest) {
        if (userCisRequest.stream().anyMatch(user -> StringUtils.isEmpty(user.getCode()))) {
            throw new BadRequestException("CI codes cannot be empty.");
        }
    }

    @Override
    public void addUserMitigation(
            String userId, String ci, UserMitigationRequest userMitigationRequest) {
        List<CimitStubItem> cimitStubItems = cimitStubService.getCimitStubItems(userId);
        Optional<CimitStubItem> cimitStubItem = getUserIdAndCodeFromDatabase(cimitStubItems, ci);
        if (cimitStubItem.isPresent()) {
            cimitStubItem
                    .get()
                    .setMitigations(
                            getUpdatedMitigationsList(
                                    cimitStubItem.get().getMitigations(),
                                    userMitigationRequest.getMitigations()));
            cimitStubService.updateCimitStub(cimitStubItem.get());
            LOGGER.info("Inserted mitigations to the Cimit Stub DynamoDB Table.");
        } else {
            throw new DataNotFoundException("User and ContraIndicator not found.");
        }
    }

    private List<String> getUpdatedMitigationsList(
            List<String> existingMitigations, List<String> newMitigations) {
        Stream<String> combinedStream = Stream.empty();
        if (existingMitigations != null ) {
            combinedStream = Stream.concat(combinedStream, existingMitigations.stream());
        }
        if (newMitigations != null) {
            combinedStream = Stream.concat(combinedStream, newMitigations.stream());
        }
        return combinedStream
                .distinct()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    @Override
    public void updateUserMitigation(
            String userId, String ci, UserMitigationRequest userMitigationRequest) {
        List<CimitStubItem> cimitStubItems = cimitStubService.getCimitStubItems(userId);
        Optional<CimitStubItem> cimitStubItem = getUserIdAndCodeFromDatabase(cimitStubItems, ci);
        if (cimitStubItem.isPresent()) {
            cimitStubItem.get().setMitigations(userMitigationRequest.getMitigations());
            cimitStubService.updateCimitStub(cimitStubItem.get());
            LOGGER.info("Updated mitigations to the Cimit Stub DynamoDB Table.");
        } else {
            throw new DataNotFoundException("User and ContraIndicator not found.");
        }
    }

    private Instant getIssuanceDate(String issuanceDate) {
        if (!StringUtils.isEmpty(issuanceDate)) {
            return Instant.parse(issuanceDate);
        }
        return Instant.now();
    }

    private Optional<CimitStubItem> getUserIdAndCodeFromDatabase(
            List<CimitStubItem> cimitStubItems, String code) {
        return cimitStubItems.stream()
                .filter(cimitStubItem -> cimitStubItem.getContraIndicatorCode().equals(code))
                .findAny();
    }

    public List<String> convertListToUppercase(List<String> codes){
        if (codes != null && !codes.isEmpty()){
            return codes.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
        }
        return codes;
    }
}
