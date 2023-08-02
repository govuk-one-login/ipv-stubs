package uk.gov.di.ipv.core.postmitigations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PostMitigationsHandler implements RequestStreamHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String FAILURE_RESPONSE = "Failure";
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        LOGGER.info(new StringMapMessage().with("Function invoked:", "PostMitigations"));

        String response;
        try {
            mapper.readValue(input, PostMitigationsRequest.class);
            response = "Success";
        } catch (Exception e) {
            LOGGER.error(
                    new StringMapMessage().with("Unable to parse input request", e.getMessage()));
            response = FAILURE_RESPONSE;
        }
        mapper.writeValue(output, response);
    }
}
