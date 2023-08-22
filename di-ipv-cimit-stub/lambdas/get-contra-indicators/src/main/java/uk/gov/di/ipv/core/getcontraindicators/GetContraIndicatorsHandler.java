package uk.gov.di.ipv.core.getcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.getcontraindicators.domain.ContraIndicatorItem;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiRequest;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiResponse;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

public class GetContraIndicatorsHandler implements RequestStreamHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ConfigService configService;
    private final CimitStubItemService cimitStubItemService;

    public GetContraIndicatorsHandler() {
        this.configService = new ConfigService();
        this.cimitStubItemService = new CimitStubItemService(configService);
    }

    public GetContraIndicatorsHandler(
            ConfigService configService, CimitStubItemService cimitStubItemService) {
        this.configService = configService;
        this.cimitStubItemService = cimitStubItemService;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        LOGGER.info(new StringMapMessage().with("Function invoked:", "GetContraIndicators"));

        GetCiRequest getCiRequest;
        GetCiResponse response;
        try {
            getCiRequest = mapper.readValue(input, GetCiRequest.class);
        } catch (Exception ex) {
            LOGGER.error(
                    new StringMapMessage().with("Unable to parse input request", ex.getMessage()));
            throw ex;
        }

        // Initially returning empty CI's
        response = new GetCiResponse(getContraIndicators(getCiRequest.getUserId()));

        mapper.writeValue(output, response);
    }

    private List<ContraIndicatorItem> getContraIndicators(String userId) {
        List<CimitStubItem> cimitStubItems = cimitStubItemService.getCIsForUserId(userId);
        return cimitStubItems.stream()
                .map(
                        item ->
                                ContraIndicatorItem.builder()
                                        .ci(item.getContraIndicatorCode())
                                        .ttl(Long.toString(item.getTtl()))
                                        .iss(configService.getCimitComponentId())
                                        .userId(userId)
                                        .build())
                .collect(Collectors.toList());
    }
}
