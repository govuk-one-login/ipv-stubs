package uk.gov.di.ipv.core.getcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiRequest;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

public class GetContraIndicatorsHandler implements RequestStreamHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        LOGGER.info(new StringMapMessage().with("Function invoked:", "GetContraIndicators"));

        GetCiResponse response;
        try {
            mapper.readValue(input, GetCiRequest.class);
        } catch (Exception ex) {
            LOGGER.error(
                    new StringMapMessage().with("Unable to parse input request", ex.getMessage()));
            throw ex;
        }

        // Initially returning empty CI's
        response = new GetCiResponse(Collections.emptyList());

        mapper.writeValue(output, response);
    }
}
