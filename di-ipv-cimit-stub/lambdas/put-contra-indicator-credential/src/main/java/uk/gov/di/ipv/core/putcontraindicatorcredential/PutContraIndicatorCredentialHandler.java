package uk.gov.di.ipv.core.putcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.core.putcontraindicatorcredential.domain.PutContraIndicatorCredentialRequest;

public class PutContraIndicatorCredentialHandler implements RequestHandler<PutContraIndicatorCredentialRequest, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutContraIndicatorCredentialHandler.class);

    @Override
    public String handleRequest(PutContraIndicatorCredentialRequest event, Context context) {
        LOGGER.info("EVENT TYPE: " + event.getClass().toString());
        return "Success";
    }
}
