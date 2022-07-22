package uk.gov.di.ipv.stub.fraud;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.*;
import uk.gov.di.ipv.stub.fraud.gateway.dto.response.IdentityVerificationResponse;

import java.io.*;
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

    protected Route tokenRequest =
            (Request request, Response response) -> {
                LOGGER.info("tokenRequest body: " + request.body());

                IdentityVerificationRequest fraudRequest =
                        mapper.readValue(request.body(), IdentityVerificationRequest.class);

                Contact requestContact = fraudRequest.getPayload().getContacts().get(0);
                String requestDob = requestContact.getPerson().getPersonDetails().getDateOfBirth();
                List<Name> requestNames = requestContact.getPerson().getNames();
                List<Address> requestAddress = requestContact.getAddresses();

                IdentityVerificationResponse experianResponse =
                        inMemoryDataStore.getOrElse(
                                requestNames.get(0).getSurName().toUpperCase(),
                                inMemoryDataStore.get("AUTH1"));
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

                if (requestNames.get(0).getSurName().toUpperCase().equals("SERVER_FAILURE")) {
                    response.status(503);
                    return "";
                } else {
                    response.header("Content-Type", "application/json");
                    response.status(200);
                    return mapper.writeValueAsString(experianResponse);
                }
            };
}
