package uk.gov.di.ipv.core.getcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiRequest;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiResponse;

import java.util.Collections;

public class GetContraIndicatorsHandler implements RequestHandler<GetCiRequest, GetCiResponse> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public GetCiResponse handleRequest(GetCiRequest event, Context context) {
        LOGGER.info(new StringMapMessage().with("EVENT TYPE:", event.getClass().toString()));
        return new GetCiResponse(Collections.emptyList());
    }
}
