package uk.gov.di.ipv.core.postmitigations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsRequest;

public class PostMitigationsHandler implements RequestHandler<PostMitigationsRequest, String> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String handleRequest(PostMitigationsRequest event, Context context) {
        LOGGER.info(new StringMapMessage().with("EVENT TYPE:", event.getClass().toString()));
        return "Success";
    }
}
