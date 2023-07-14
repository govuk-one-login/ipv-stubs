package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetContraIndicatorCredentialHandler
        implements RequestHandler<Map<String, String>, Void> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GetContraIndicatorCredentialHandler.class);

    @Override
    public Void handleRequest(Map<String, String> event, Context context) {
        LambdaLogger logger = context.getLogger();
        LOGGER.info("EVENT TYPE: " + event.getClass());
        return null;
    }
}
