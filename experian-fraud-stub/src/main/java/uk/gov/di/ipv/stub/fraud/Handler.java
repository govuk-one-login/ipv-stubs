package uk.gov.di.ipv.stub.fraud;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.*;
import uk.gov.di.ipv.stub.fraud.gateway.dto.response.IdentityVerificationResponse;

import java.util.*;

public class Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Handler.class);
    private ObjectMapper mapper;
    private InMemoryDataStore inMemoryDataStore;

    protected Handler() {
        mapper = new ObjectMapper();
        inMemoryDataStore = new InMemoryDataStore(mapper);
    }

    protected Route root = (Request request, Response response) -> "ok";

    protected Route fraudCheck =
            (Request request, Response response) -> {
                LOGGER.info("Fraud request: " + request.body());

                IdentityVerificationRequest fraudRequest =
                        mapper.readValue(request.body(), IdentityVerificationRequest.class);

                Contact requestContact = fraudRequest.getPayload().getContacts().get(0);
                String requestDob = requestContact.getPerson().getPersonDetails().getDateOfBirth();
                List<Name> requestNames = requestContact.getPerson().getNames();
                List<Address> requestAddress = requestContact.getAddresses();

                IdentityVerificationResponse experianResponse =
                        inMemoryDataStore.getResponseOrElse(
                                requestNames.get(0).getSurName().toUpperCase(),
                                inMemoryDataStore.getResponse("AUTH1"));
                if (fraudRequest.getHeader().getRequestType().equals("PepSanctions01")) {
                    experianResponse =
                            inMemoryDataStore.getResponseOrElse(
                                    requestNames.get(0).getSurName().toUpperCase(),
                                    inMemoryDataStore.getResponse("PEPS-NO-RULE"));
                }

                LOGGER.debug("Stubbed experian response = " + experianResponse);

                Random randGen = new Random();
                experianResponse
                        .getResponseHeader()
                        .setClientReferenceId(UUID.randomUUID().toString());

                experianResponse
                        .getResponseHeader()
                        .setExpRequestId(String.format("RB0000%08d", randGen.nextInt(99999999)));

                Contact responseContact =
                        experianResponse.getOriginalRequestData().getContacts().get(0);
                Person responseContactPerson = responseContact.getPerson();

                responseContact.setAddresses(requestAddress);

                responseContactPerson.setNames(requestNames);
                responseContactPerson.getPersonDetails().setDateOfBirth(requestDob);

                if (requestNames.get(0).getSurName().equalsIgnoreCase("SERVER_FAILURE")) {
                    response.status(503);
                    return "";
                } else {
                    response.header("Content-Type", "application/json");
                    response.status(200);
                    return mapper.writeValueAsString(experianResponse);
                }
            };

    protected Route addFraudResponse =
            (Request request, Response response) -> {
                LOGGER.info("identity verification response: " + request.body());

                IdentityVerificationResponse experianResponse =
                        mapper.readValue(request.body(), IdentityVerificationResponse.class);
                inMemoryDataStore.addResponse(
                        experianResponse
                                .getOriginalRequestData()
                                .getContacts()
                                .get(0)
                                .getPerson()
                                .getNames()
                                .get(0)
                                .getSurName()
                                .toUpperCase(),
                        experianResponse);

                IdentityVerificationRequest fraudRequest = new IdentityVerificationRequest();
                Header header = new Header();
                Payload payload = new Payload();
                header.setRequestType(experianResponse.getResponseHeader().getRequestType());
                payload.setContacts(experianResponse.getOriginalRequestData().getContacts());

                fraudRequest.setHeader(header);
                fraudRequest.setPayload(payload);

                return mapper.writeValueAsString(fraudRequest);
            };

    protected Route deleteFraudResponse =
            (Request request, Response response) -> {
                LOGGER.info("Fraud request: " + request.body());

                Map<String, String> deletionRequest = mapper.readValue(request.body(), Map.class);

                String id = deletionRequest.get("fraudResponseId");

                boolean responseRemoved = inMemoryDataStore.removeResponse(id.toUpperCase());
                return responseRemoved
                        ? String.format("%s removed", id)
                        : String.format("%s does not exist", id);
            };

    protected Route getFraudResponse =
            (Request request, Response response) -> {
                String fraudResponseId = request.queryParams("fraudResponseId");
                LOGGER.info("Fraud request ID: " + fraudResponseId);

                response.header("Content-Type", "application/json");
                return mapper.writeValueAsString(inMemoryDataStore.getResponse(fraudResponseId));
            };
}
