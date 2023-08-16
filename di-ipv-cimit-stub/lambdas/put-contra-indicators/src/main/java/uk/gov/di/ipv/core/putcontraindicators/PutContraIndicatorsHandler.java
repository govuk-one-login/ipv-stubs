package uk.gov.di.ipv.core.putcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequest;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsResponse;
import uk.gov.di.ipv.core.putcontraindicators.exceptions.CiPutException;
import uk.gov.di.ipv.core.putcontraindicators.service.ContraIndicatorsService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PutContraIndicatorsHandler implements RequestStreamHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ContraIndicatorsService cimitService;

    public PutContraIndicatorsHandler() {
        this.cimitService = new ContraIndicatorsService();
    }

    public PutContraIndicatorsHandler(ContraIndicatorsService cimitService) {
        this.cimitService = cimitService;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        LOGGER.info(new StringMapMessage().with("Function invoked:", "PutContraIndicators"));
        PutContraIndicatorsRequest putContraIndicatorsRequest;
        PutContraIndicatorsResponse response;

        try {
            putContraIndicatorsRequest = mapper.readValue(input, PutContraIndicatorsRequest.class);
        } catch (IOException ex) {
            LOGGER.error(
                    new StringMapMessage().with("Unable to parse input request", ex.getMessage()));
            throw ex;
        }
        try {
            cimitService.addUserCis(putContraIndicatorsRequest);
            response = PutContraIndicatorsResponse.builder().result("success").build();
        } catch (CiPutException ex) {
            LOGGER.error(
                    new StringMapMessage()
                            .with(
                                    "CI Codes could not be inserted into the Cimit Stub Table.",
                                    ex.getMessage()));
            response = PutContraIndicatorsResponse.builder().result("fail").build();
        }
        mapper.writeValue(output, response);
    }
}
