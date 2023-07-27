package uk.gov.di.ipv.core.stubmanagement.service;

import uk.gov.di.ipv.core.stubmanagement.model.UserCisRequest;
import uk.gov.di.ipv.core.stubmanagement.model.UserMitigationRequest;

import java.util.List;

public interface UserService {

    void addUserCis(String userId, List<UserCisRequest> userCisRequest);

    void updateUserCis(String userId, List<UserCisRequest> userCisRequest);

    void addUserMitigation(String userId, String ci, UserMitigationRequest userMitigationRequest);

    void updateUserMitigation(
            String userId, String ci, UserMitigationRequest userMitigationRequest);
}
