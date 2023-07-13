package uk.gov.di.ipv.core.getcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiRequest;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiResponse;

import java.util.Collections;

public class GetContraIndicatorsHandler implements RequestHandler<GetCiRequest, GetCiResponse> {

    @Override
    public GetCiResponse handleRequest(GetCiRequest event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("EVENT TYPE: " + event.getClass().toString());
        return new GetCiResponse(Collections.emptyList());
    }
}
