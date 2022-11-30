package uk.gov.di.ipv.stub.fraud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.*;
import uk.gov.di.ipv.stub.fraud.gateway.dto.response.IdentityVerificationResponse;
import uk.gov.di.ipv.stub.fraud.gateway.dto.response.ResponseType;

import java.util.*;

public class Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Handler.class);
    private ObjectMapper mapper;

    public static final String FRAUD_CHECK_SOURCE = "fraud_check_source";
    private InMemoryDataStore fraudCheckInMemoryDataStore;

    public static final String PEP_CHECK_SOURCE = "pep_check_source";
    private InMemoryDataStore pepCheckInMemoryDataStore;

    protected Handler() {
        mapper = new ObjectMapper();
        fraudCheckInMemoryDataStore = new InMemoryDataStore(mapper, FRAUD_CHECK_SOURCE);
        pepCheckInMemoryDataStore = new InMemoryDataStore(mapper, PEP_CHECK_SOURCE);
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

                IdentityVerificationResponse modifiableResponse = null;

                // FraudCheck Simulation
                if (fraudRequest
                        .getHeader()
                        .getRequestType()
                        .equals("Authenticateplus-Standalone")) {

                    // Look for a response for specific user surname, fall back to AUTH1
                    modifiableResponse =
                            getModifiableResponse(
                                    requestNames.get(0).getSurName().toUpperCase(),
                                    "AUTH1",
                                    FRAUD_CHECK_SOURCE);

                    // Decision score set to suffix
                    if (requestNames.get(0).getSurName().contains("NO_FILE_")) {
                        modifiableResponse
                                .getClientResponsePayload()
                                .getDecisionElements()
                                .get(0)
                                .setScore(
                                        Integer.valueOf(
                                                requestNames.get(0).getSurName().substring(8)));
                    }

                    // Error response type in FraudCheck
                    if (requestNames.get(0).getSurName().equals("FRAUD_ERROR_RESPONSE")) {
                        modifiableResponse.getResponseHeader().setResponseType(ResponseType.ERROR);
                        modifiableResponse.getResponseHeader().setResponseCode("ERRSIMF1");
                        modifiableResponse
                                .getResponseHeader()
                                .setResponseMessage("Simulated Fraud Error Response");
                    }

                    // HTTP response status code to fraudCheck
                    if (requestNames.get(0).getSurName().contains("FSC_")) {
                        response.status(
                                Integer.valueOf(requestNames.get(0).getSurName().substring(4)));
                    }

                    if (requestNames.get(0).getSurName().equals("FRAUD_TECH_FAIL")) {
                        response.status(
                                408); // Request Timeout (closest response to an abrupt socket
                        // close)
                        return "";
                    }
                }

                // PepCheck Simulation
                if (fraudRequest.getHeader().getRequestType().equals("PepSanctions01")) {

                    modifiableResponse =
                            getModifiableResponse(
                                    requestNames.get(0).getSurName().toUpperCase(),
                                    "PEPS-NO-RULE",
                                    PEP_CHECK_SOURCE);

                    // Error response type in PEP Check
                    if (requestNames.get(0).getSurName().equals("PEP_ERROR_RESPONSE")) {
                        modifiableResponse.getResponseHeader().setResponseType(ResponseType.ERROR);
                        modifiableResponse.getResponseHeader().setResponseCode("ERRSIMP1");
                        modifiableResponse
                                .getResponseHeader()
                                .setResponseMessage("Simulated PEP Error Response");
                    }

                    // HTTP response status code to fraudCheck
                    if (requestNames.get(0).getSurName().contains("PSC_")) {
                        response.status(
                                Integer.valueOf(requestNames.get(0).getSurName().substring(4)));
                    }

                    if (requestNames.get(0).getSurName().equals("PEP_TECH_FAIL")) {
                        response.status(
                                408); // Request Timeout (closest response to an abrupt socket
                        // close)
                        return "";
                    }
                }

                LOGGER.debug("Stubbed experian response = " + modifiableResponse);

                Random randGen = new Random();
                modifiableResponse
                        .getResponseHeader()
                        .setClientReferenceId(UUID.randomUUID().toString());

                modifiableResponse
                        .getResponseHeader()
                        .setExpRequestId(String.format("RB0000%08d", randGen.nextInt(99999999)));

                Contact responseContact =
                        modifiableResponse.getOriginalRequestData().getContacts().get(0);
                Person responseContactPerson = responseContact.getPerson();

                responseContact.setAddresses(requestAddress);

                responseContactPerson.setNames(requestNames);
                responseContactPerson.getPersonDetails().setDateOfBirth(requestDob);

                if (requestNames.get(0).getSurName().equalsIgnoreCase("SERVER_FAILURE")) {
                    response.status(503);
                    return "";
                } else {
                    response.header("Content-Type", "application/json");
                    // status code 200 by default
                    return mapper.writeValueAsString(modifiableResponse);
                }
            };

    protected Route addFraudResponse =
            (Request request, Response response) -> {
                LOGGER.info("identity verification response: " + request.body());

                IdentityVerificationResponse experianResponse =
                        mapper.readValue(request.body(), IdentityVerificationResponse.class);
                fraudCheckInMemoryDataStore.addResponse(
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

                boolean responseRemoved =
                        fraudCheckInMemoryDataStore.removeResponse(id.toUpperCase());
                return responseRemoved
                        ? String.format("%s removed", id)
                        : String.format("%s does not exist", id);
            };

    protected Route getFraudResponse =
            (Request request, Response response) -> {
                String fraudResponseId = request.queryParams("fraudResponseId");
                LOGGER.info("Fraud request ID: " + fraudResponseId);

                response.header("Content-Type", "application/json");
                return mapper.writeValueAsString(
                        fraudCheckInMemoryDataStore.getResponse(fraudResponseId));
            };

    private IdentityVerificationResponse getModifiableResponse(
            String surname, String getResponseId, String responseSource)
            throws JsonProcessingException {

        final IdentityVerificationResponse templateResponse;

        switch (responseSource) {
            case FRAUD_CHECK_SOURCE:
                templateResponse =
                        fraudCheckInMemoryDataStore.getResponseOrElse(
                                surname, fraudCheckInMemoryDataStore.getResponse(getResponseId));
                break;
            case PEP_CHECK_SOURCE:
                templateResponse =
                        pepCheckInMemoryDataStore.getResponseOrElse(
                                surname, pepCheckInMemoryDataStore.getResponse(getResponseId));
                break;
            default:
                templateResponse = null;
                LOGGER.error("ResponseSource {} is not handled", responseSource);
                break;
        }

        // Return a modifiable clone of the templateResponse
        return mapper.readValue(
                mapper.writeValueAsString(templateResponse), IdentityVerificationResponse.class);
    }
}
