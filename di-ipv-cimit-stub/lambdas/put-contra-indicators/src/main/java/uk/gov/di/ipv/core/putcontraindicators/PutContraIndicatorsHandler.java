package uk.gov.di.ipv.core.putcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequest;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsResponse;
import com.google.gson.Gson;

public class PutContraIndicatorsHandler
        implements RequestHandler<PutContraIndicatorsRequest, String> {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson gson = new Gson();
    @Override
    public String handleRequest(PutContraIndicatorsRequest event, Context context) {
        LOGGER.info(new StringMapMessage().with("EVENT TYPE:", event.getClass().toString()));
        return gson.toJson(PutContraIndicatorsResponse.builder().result("success").build());
    }
}
