package uk.gov.di.ipv.core.stubmanagement.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.utils.StringUtils;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.stubmanagement.exceptions.BadRequestException;
import uk.gov.di.ipv.core.stubmanagement.model.UserCisRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ConfigService configService;
    private final CimitStubItemService cimitStubItemService;

    public UserService() {
        this.configService = new ConfigService();
        this.cimitStubItemService = new CimitStubItemService(configService);
    }

    public UserService(ConfigService configService, CimitStubItemService cimitStubItemService) {
        this.configService = configService;
        this.cimitStubItemService = cimitStubItemService;
    }

    public void addUserCis(String userId, List<UserCisRequest> userCisRequest) {
        checkCICodes(userCisRequest);
        List<CimitStubItem> cimitStubItems = cimitStubItemService.getCIsForUserId(userId);
        userCisRequest.forEach(
                request -> {
                    Optional<CimitStubItem> cimitStubItem =
                            getCimitStubItemByCiCode(
                                    cimitStubItems, request.getCode().toUpperCase());
                    if (cimitStubItem.isEmpty()) {
                        cimitStubItemService.persistCimitStub(
                                userId,
                                request.getCode().toUpperCase(),
                                request.getIssuers(),
                                getIssuanceDate(request.getIssuanceDate()),
                                convertListToUppercase(request.getMitigations()));
                    } else {
                        CimitStubItem item = cimitStubItem.get();
                        item.setMitigations(
                                getUpdatedMitigationsList(
                                        item.getMitigations(), request.getMitigations()));
                        item.setIssuers(combineLists(item.getIssuers(), request.getIssuers()));
                        item.setIssuanceDate(getIssuanceDate(request.getIssuanceDate()));
                        cimitStubItemService.updateCimitStubItem(item);
                    }
                });
        LOGGER.info("Inserted User CI data to the Cimit Stub DynamoDB Table.");
    }

    public void updateUserCis(String userId, List<UserCisRequest> userCisRequest) {
        checkCICodes(userCisRequest);
        List<CimitStubItem> cimitStubItems = cimitStubItemService.getCIsForUserId(userId);
        if (!cimitStubItems.isEmpty()) {
            deleteCimitStubItems(cimitStubItems);
        }
        userCisRequest.forEach(
                request -> {
                    cimitStubItemService.persistCimitStub(
                            userId,
                            request.getCode().toUpperCase(),
                            request.getIssuers(),
                            getIssuanceDate(request.getIssuanceDate()),
                            convertListToUppercase(request.getMitigations()));
                });
    }

    private void deleteCimitStubItems(List<CimitStubItem> cimitStubItems) {
        cimitStubItems.stream()
                .forEach(
                        item ->
                                cimitStubItemService.deleteCimitStubItem(
                                        item.getUserId(), item.getContraIndicatorCode()));
    }

    private static void checkCICodes(List<UserCisRequest> userCisRequest) {
        if (userCisRequest.stream().anyMatch(user -> StringUtils.isEmpty(user.getCode()))) {
            throw new BadRequestException("CI codes cannot be empty.");
        }
    }

    private List<String> getUpdatedMitigationsList(
            List<String> existingMitigations, List<String> newMitigations) {
        return combineLists(existingMitigations, newMitigations).stream()
                .distinct()
                .map(String::toUpperCase)
                .toList();
    }

    private <T> List<T> combineLists(List<T> firstList, List<T> secondList) {
        Stream<T> combinedStream = Stream.empty();
        if (firstList != null) {
            combinedStream = Stream.concat(combinedStream, firstList.stream());
        }
        if (secondList != null) {
            combinedStream = Stream.concat(combinedStream, secondList.stream());
        }

        return combinedStream.toList();
    }

    private Instant getIssuanceDate(String issuanceDate) {
        if (!StringUtils.isEmpty(issuanceDate)) {
            return Instant.parse(issuanceDate);
        }
        return Instant.now();
    }

    private Optional<CimitStubItem> getCimitStubItemByCiCode(
            List<CimitStubItem> cimitStubItems, String code) {
        return cimitStubItems.stream()
                .filter(cimitStubItem -> cimitStubItem.getContraIndicatorCode().equals(code))
                .findAny();
    }

    public List<String> convertListToUppercase(List<String> codes) {
        if (codes != null && !codes.isEmpty()) {
            return codes.stream().map(String::toUpperCase).collect(Collectors.toList());
        }
        return codes;
    }
}
