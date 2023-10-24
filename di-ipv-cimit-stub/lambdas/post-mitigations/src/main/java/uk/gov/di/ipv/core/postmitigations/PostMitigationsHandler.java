package uk.gov.di.ipv.core.postmitigations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.library.service.PendingMitigationService;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;

public class PostMitigationsHandler implements RequestStreamHandler {

    public static final String FAILURE_RESPONSE = "Failure";
    public static final String SUCCESS_RESPONSE = "Success";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final CimitStubItemService cimitStubItemService;
    private final PendingMitigationService pendingMitigationService;

    public PostMitigationsHandler() {
        ConfigService configService = new ConfigService();
        this.pendingMitigationService = new PendingMitigationService(configService);
        this.cimitStubItemService = new CimitStubItemService(configService);
    }

    public PostMitigationsHandler(
            PendingMitigationService pendingMitigationService,
            CimitStubItemService cimitStubItemService) {
        this.pendingMitigationService = pendingMitigationService;
        this.cimitStubItemService = cimitStubItemService;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        LOGGER.info(new StringMapMessage().with("Function invoked:", "PostMitigations"));

        String response;
        try {
            PostMitigationsRequest postMitigationsRequest =
                    MAPPER.readValue(input, PostMitigationsRequest.class);
            for (String vc : postMitigationsRequest.getSignedJwtVCs()) {
                JWTClaimsSet jwtClaimsSet = SignedJWT.parse(vc).getJWTClaimsSet();
                pendingMitigationService.completePendingMitigation(
                        jwtClaimsSet.getJWTID(), jwtClaimsSet.getSubject(), cimitStubItemService);
            }
            response = SUCCESS_RESPONSE;
        } catch (ParseException | IOException e) {
            LOGGER.error(
                    new StringMapMessage().with("Unable to parse input request", e.getMessage()));
            response = FAILURE_RESPONSE;
        }
        MAPPER.writeValue(output, response);
    }
}
