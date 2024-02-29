package uk.gov.di.ipv.core.stubmanagement.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.utils.StringUtils;
import uk.gov.di.ipv.core.library.model.UserCisRequest;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.stubmanagement.exceptions.BadRequestException;

import java.util.List;

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
        userCisRequest.forEach(
                ciRequest ->
                        cimitStubItemService.persistCimitStubItem(
                                CimitStubItem.fromUserCiRequest(ciRequest, userId)));
        LOGGER.info("Inserted User CI data to the Cimit Stub DynamoDB Table.");
    }

    public void updateUserCis(String userId, List<UserCisRequest> userCisRequest) {
        checkCICodes(userCisRequest);
        List<CimitStubItem> cimitStubItems = cimitStubItemService.getCIsForUserId(userId);
        if (!cimitStubItems.isEmpty()) {
            deleteCimitStubItems(cimitStubItems);
        }
        userCisRequest.forEach(
                ciRequest ->
                        cimitStubItemService.persistCimitStubItem(
                                CimitStubItem.fromUserCiRequest(ciRequest, userId)));
    }

    private void deleteCimitStubItems(List<CimitStubItem> cimitStubItems) {
        cimitStubItems.forEach(
                item ->
                        cimitStubItemService.deleteCimitStubItem(
                                item.getUserId(), item.getContraIndicatorCode()));
    }

    private static void checkCICodes(List<UserCisRequest> userCisRequest) {
        if (userCisRequest.stream().anyMatch(user -> StringUtils.isEmpty(user.getCode()))) {
            throw new BadRequestException("CI codes cannot be empty.");
        }
    }
}
