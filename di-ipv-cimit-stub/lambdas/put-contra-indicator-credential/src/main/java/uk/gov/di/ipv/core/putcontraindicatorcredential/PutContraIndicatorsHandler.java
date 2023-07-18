package uk.gov.di.ipv.core.putcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.putcontraindicatorcredential.domain.PutContraIndicatorsRequest;

public class PutContraIndicatorsHandler
        implements RequestHandler<PutContraIndicatorsRequest, String> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String handleRequest(PutContraIndicatorsRequest event, Context context) {
        LOGGER.info(new StringMapMessage().with("EVENT TYPE:", event.getClass().toString()));
        return "Success";
    }
}
