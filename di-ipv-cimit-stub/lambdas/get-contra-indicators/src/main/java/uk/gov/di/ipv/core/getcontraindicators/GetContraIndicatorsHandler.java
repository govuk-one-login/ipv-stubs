package uk.gov.di.ipv.core.getcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiRequest;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiResponse;

import java.util.Collections;

public class GetContraIndicatorsHandler implements RequestHandler<GetCiRequest, GetCiResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetContraIndicatorsHandler.class);

    @Override
    public GetCiResponse handleRequest(GetCiRequest event, Context context) {
        LOGGER.info("EVENT TYPE: " + event.getClass().toString());
        return new GetCiResponse(Collections.emptyList());
    }
}