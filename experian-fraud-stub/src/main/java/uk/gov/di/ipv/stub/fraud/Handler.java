package uk.gov.di.ipv.stub.fraud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.*;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.Contact;
import uk.gov.di.ipv.stub.fraud.gateway.dto.response.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Handler.class);
    private ObjectMapper mapper;

    public static final String FRAUD_CHECK_SOURCE = "fraud_check_source";
    private InMemoryDataStore fraudCheckInMemoryDataStore;

    public static final String PEP_CHECK_SOURCE = "pep_check_source";
    private InMemoryDataStore pepCheckInMemoryDataStore;

    // To prevent accidentally requesting the stub to wait very long
    private static long MAX_DELAYED_RESPONSE_MS = 30000;

    protected Handler() {
        mapper = new ObjectMapper();
        fraudCheckInMemoryDataStore = new InMemoryDataStore(mapper, FRAUD_CHECK_SOURCE);
        pepCheckInMemoryDataStore = new InMemoryDataStore(mapper, PEP_CHECK_SOURCE);
    }

    protected Route root = (Request request, Response response) -> "ok";

    protected Route fraudCheck =
            (Request request, Response response) -> {
                LOGGER.info("Fraud request: " + request.body());

                IdentityVerificationRequest identityVerificationRequest =
                        mapper.readValue(request.body(), IdentityVerificationRequest.class);

                Contact requestContact =
                        identityVerificationRequest.getPayload().getContacts().get(0);
                String requestDob = requestContact.getPerson().getPersonDetails().getDateOfBirth();
                List<Name> requestNames = requestContact.getPerson().getNames();
                List<Address> requestAddress = requestContact.getAddresses();

                final String requestType = identityVerificationRequest.getHeader().getRequestType();
                final String requestSurnameName =
                        requestNames.get(0).getSurName() != null
                                ? requestNames.get(0).getSurName().toUpperCase()
                                : "";

                IdentityVerificationResponse modifiableResponse = null;

                if (requestType.equals("Authenticateplus-Standalone")) {
                    // FraudCheck Simulation

                    // Look for a response for specific user surname, fall back to AUTH1
                    modifiableResponse =
                            getModifiableResponse(requestSurnameName, "AUTH1", FRAUD_CHECK_SOURCE);

                    // FRAUD_WARNINGS_ERRORS is based on real info response with 1 warningError
                    if (requestSurnameName.equals("FRAUD_WARNINGS_ERRORS")) {
                        modifiableResponse =
                                getModifiableResponse(
                                        requestSurnameName,
                                        "FRAUD_WARNINGS_ERRORS",
                                        FRAUD_CHECK_SOURCE);
                    } else if (!checkRequestContactHasRequiredData(requestContact)) {
                        // Request is missing fields needed for a real request

                        modifiableResponse.getResponseHeader().setResponseType(ResponseType.ERROR);
                        modifiableResponse.getResponseHeader().setResponseCode("ERRSIMD1");
                        modifiableResponse
                                .getResponseHeader()
                                .setResponseMessage(
                                        "Simulated - RequestContact is missing data for a required field");
                    }

                    // Decision score set to suffix
                    if (requestSurnameName.contains("NO_FILE_")) {
                        modifiableResponse
                                .getClientResponsePayload()
                                .getDecisionElements()
                                .get(0)
                                .setScore(Integer.valueOf(requestSurnameName.substring(8)));
                    }

                    // Error response type in FraudCheck
                    if (requestSurnameName.equals("FRAUD_ERROR_RESPONSE")) {
                        modifiableResponse.getResponseHeader().setResponseType(ResponseType.ERROR);
                        modifiableResponse.getResponseHeader().setResponseCode("ERRSIMDF1");
                        modifiableResponse
                                .getResponseHeader()
                                .setResponseMessage("Simulated Fraud Error Response");
                    }

                    // HTTP response status code to fraudCheck
                    if (requestSurnameName.contains("FSC_")) {
                        response.status(Integer.valueOf(requestSurnameName.substring(4)));
                    }

                    if (requestSurnameName.equals("FRAUD_TECH_FAIL")) {
                        response.status(
                                408); // Request Timeout (closest response to an abrupt socket
                        // close)
                        return ""; // No message returned intended
                    }
                    // Activity history score default success scenario
                    if (requestSurnameName.contains("AHS")) {

                        modifiableResponse =
                                getModifiableResponse(
                                        requestSurnameName, "AHS", FRAUD_CHECK_SOURCE);
                    }
                    // Activity History Scenario to return a date specified in the surname as oldest
                    // TODO: incorporate specified date test user for activity history scoore
                    /*
                    if (requestSurnameName.contains("SHS_")) {
                        AuthConsumer authConsumer =
                                modifiableResponse
                                        .getClientResponsePayload()
                                        .getDecisionElements()
                                        .get(0)
                                        .getOtherData()
                                        .getAuthResults()
                                        .getAuthPlusResults()
                                        .getAuthConsumer();

                        authConsumer
                                .getLocDataOnlyAtCLoc()
                                .setStartDateOldestPrim(requestSurnameName.substring(4));
                    }

                     */

                    // Wait x millis before replying to Fraud Check
                    if (requestSurnameName.contains("FWAIT_")) {

                        String sWait = requestSurnameName.substring(6);

                        long fwait = Math.min(Long.parseLong(sWait), MAX_DELAYED_RESPONSE_MS);

                        Thread.sleep(fwait);
                        response.status(200);
                    }
                } else if (requestType.equals("PepSanctions01")) {
                    // PepCheck Simulation

                    // Look for a response for specific user surname, Fall back to PEP-NO-RULE
                    modifiableResponse =
                            getModifiableResponse(
                                    requestSurnameName, "PEPS-NO-RULE", PEP_CHECK_SOURCE);

                    // PEP_WARNINGS_ERRORS is based on a real info response which had 2 warningError
                    // (Response code and messages changed)
                    // Position (0) was the overall error for a submission error in pep
                    // Position (1) will be specific to the error encountered
                    if (requestSurnameName.equals("PEP_WARNINGS_ERRORS")) {
                        modifiableResponse =
                                getModifiableResponse("", "PEP_WARNINGS_ERRORS", PEP_CHECK_SOURCE);
                    } else if (!checkRequestContactHasRequiredData(requestContact)) {
                        // Request is missing fields needed for a real request

                        modifiableResponse.getResponseHeader().setResponseType(ResponseType.ERROR);
                        modifiableResponse.getResponseHeader().setResponseCode("ERRSIMDP1");
                        modifiableResponse
                                .getResponseHeader()
                                .setResponseMessage(
                                        "Simulated - RequestContact is missing data for a required field");

                    } else if (requestAddress.size() > 1) {
                        // Pep check requires there be only one address so this simulates the
                        // warnings error response that would be received

                        modifiableResponse =
                                getModifiableResponse("", "PEP_WARNINGS_ERRORS", PEP_CHECK_SOURCE);

                        DecisionElement decisionElement =
                                modifiableResponse
                                        .getClientResponsePayload()
                                        .getDecisionElements()
                                        .get(0);

                        // Warning Errors
                        decisionElement.getWarningsErrors().get(1).setResponseCode("RAS1");
                        decisionElement
                                .getWarningsErrors()
                                .get(1)
                                .setResponseCode("Too many addresses sent in pep request.");
                    }

                    // Error response type in PEP Check
                    if (requestSurnameName.equals("PEP_ERROR_RESPONSE")) {
                        modifiableResponse.getResponseHeader().setResponseType(ResponseType.ERROR);
                        modifiableResponse.getResponseHeader().setResponseCode("ERRSIMP1");
                        modifiableResponse
                                .getResponseHeader()
                                .setResponseMessage("Simulated PEP Error Response");
                    }

                    // HTTP response status code to pepCheck
                    if (requestSurnameName.contains("PSC_")) {
                        response.status(Integer.valueOf(requestSurnameName.substring(4)));
                    }

                    if (requestSurnameName.equals("PEP_TECH_FAIL")) {
                        response.status(
                                408); // Request Timeout (closest response to an abrupt socket
                        // close)
                        return ""; // No message returned intended
                    }

                    // Wait x millis before replying to PEP Check
                    if (requestSurnameName.contains("PWAIT_")) {

                        String sWait = requestSurnameName.substring(6);

                        long pwait = Math.min(Long.parseLong(sWait), MAX_DELAYED_RESPONSE_MS);

                        Thread.sleep(pwait);
                        response.status(200);
                    }
                } else {
                    String message = String.format("Unknown Request Type %s", requestType);
                    LOGGER.error(message);
                    return message;
                }

                LOGGER.debug("Stubbed experian response = " + modifiableResponse);

                modifiableResponse
                        .getResponseHeader()
                        .setClientReferenceId(UUID.randomUUID().toString());

                // ExpRequestId maps to txn id
                modifiableResponse
                        .getResponseHeader()
                        .setExpRequestId(
                                String.format(
                                        "RB0000%08d",
                                        ThreadLocalRandom.current().nextInt(99999999)));

                // Sets values of OriginalRequestData (which may not be present for some responses)
                if (null != modifiableResponse.getOriginalRequestData()) {
                    Contact responseContact =
                            modifiableResponse.getOriginalRequestData().getContacts().get(0);
                    Person responseContactPerson = responseContact.getPerson();

                    responseContact.setAddresses(requestAddress);

                    responseContactPerson.setNames(requestNames);
                    responseContactPerson.getPersonDetails().setDateOfBirth(requestDob);
                }

                if (requestSurnameName.equalsIgnoreCase("SERVER_FAILURE")) {
                    response.status(503);
                    return "";
                } else {
                    response.header("Content-Type", "application/json; charset=utf-8");
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

    private boolean checkRequestContactHasRequiredData(Contact requestContact) {

        if (null == requestContact) {
            return false;
        }

        String requestDob = requestContact.getPerson().getPersonDetails().getDateOfBirth();
        if (null == requestDob || requestDob.isEmpty()) {
            return false;
        }

        List<Address> requestAddress = requestContact.getAddresses();
        if (null == requestAddress || requestAddress.isEmpty()) {
            return false;
        }

        // Simulates minimum required being a postcode + either building name or number
        for (Address address : requestAddress) {

            if (null == address.getPostal() || address.getPostal().isEmpty()) {
                return false;
            }

            boolean buildingNameValid =
                    !(null == address.getBuildingName() || address.getBuildingName().isEmpty());

            boolean buildingNumberValid =
                    !(null == address.getBuildingNumber() || address.getBuildingNumber().isEmpty());

            // False if both are not valid - true if one is
            if (!(buildingNameValid || buildingNumberValid)) {
                return false;
            }
        }

        List<Name> requestNames = requestContact.getPerson().getNames();

        if (null == requestNames) {
            return false;
        }

        // Middle name is not required
        for (Name name : requestNames) {
            if (null == name) {
                return false;
            }
            if (null == name.getFirstName() || name.getFirstName().isEmpty()) {
                return false;
            }
            if (null == name.getSurName() || name.getSurName().isEmpty()) {
                return false;
            }
        }

        // RequestContact has required data
        return true;
    }
}
