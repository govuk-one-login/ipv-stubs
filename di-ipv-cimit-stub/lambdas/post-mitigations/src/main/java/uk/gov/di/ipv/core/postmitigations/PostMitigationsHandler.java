package uk.gov.di.ipv.core.postmitigations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsRequest;

public class PostMitigationsHandler implements RequestHandler<PostMitigationsRequest, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostMitigationsHandler.class);

    @Override
    public String handleRequest(PostMitigationsRequest event, Context context) {
        LOGGER.info("EVENT TYPE: " + event.getClass().toString());
        return "Success";
    }
}
