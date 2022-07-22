package uk.gov.di.ipv.stub.fraud;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.fraud.gateway.dto.response.IdentityVerificationResponse;
import uk.gov.di.ipv.stub.fraud.gateway.dto.response.Rule;

import java.util.HashMap;
import java.util.List;

import static uk.gov.di.ipv.stub.fraud.Util.getResourceAsStream;
import static uk.gov.di.ipv.stub.fraud.Util.mapFileToObject;

public class InMemoryDataStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryDataStore.class);
    private HashMap<String, IdentityVerificationResponse> experianResponses = new HashMap<>();

    public InMemoryDataStore(ObjectMapper mapper) {
        init();
    }

    public IdentityVerificationResponse get(final String id) {
        return experianResponses.get(id);
    }

    public IdentityVerificationResponse getOrElse(
            final String id, final IdentityVerificationResponse alt) {
        return experianResponses.getOrDefault(id, alt);
    }

    public void put(
            final String id, final IdentityVerificationResponse identityVerificationResponse) {
        experianResponses.put(id, identityVerificationResponse);
    }

    private void init() {
        experianResponses.put(
                "AUTH1",
                mapFileToObject(
                        getResourceAsStream("/GenericResponse/fraud-ex--auth1.json"),
                        IdentityVerificationResponse.class));
        experianResponses.put(
                "AUTH2",
                mapFileToObject(
                        getResourceAsStream("/GenericResponse/fraud-ex--auth2.json"),
                        IdentityVerificationResponse.class));
        experianResponses.put(
                "NOAUTH",
                mapFileToObject(
                        getResourceAsStream("/GenericResponse/fraud-ex--noauth.json"),
                        IdentityVerificationResponse.class));
        experianResponses.put(
                "REFER",
                mapFileToObject(
                        getResourceAsStream("/GenericResponse/fraud-ex--refer.json"),
                        IdentityVerificationResponse.class));

        experianResponses.put(
                "FARRELL",
                mapFileToObject(
                        getResourceAsStream("/SpecificResponse/fraud-ex-a01-farrell.json"),
                        IdentityVerificationResponse.class));
        experianResponses.put(
                "ARKIL",
                mapFileToObject(
                        getResourceAsStream("/SpecificResponse/fraud-ex-n01-arkil.json"),
                        IdentityVerificationResponse.class));
        experianResponses.put(
                "GILT",
                mapFileToObject(
                        getResourceAsStream("/SpecificResponse/fraud-ex-t02-gilt.json"),
                        IdentityVerificationResponse.class));
        experianResponses.put(
                "KENNEDY",
                mapFileToObject(
                        getResourceAsStream("/SpecificResponse/fraud-ex-t03-kennedy.json"),
                        IdentityVerificationResponse.class));

        experianResponses.put("A01", experianResponses.get("REFER"));
        experianResponses.put("N01", experianResponses.get("REFER"));
        experianResponses.put("T02", experianResponses.get("REFER"));
        experianResponses.put("T03", experianResponses.get("REFER"));
        experianResponses.put("T05", experianResponses.get("REFER"));

        setRuleId("A01", "U150");
        setRuleId("N01", "U007", "U156", "U018");
        setRuleId("T02", "U001", "U141");
        setRuleId("T03", "U142", "U143", "U144", "U145", "U146", "U147", "U163");
        setRuleId("T05", "U160", "U161");
    }

    private void setRuleId(String ci, String... Ucode) {
        List<Rule> Rules =
                experianResponses
                        .get(ci)
                        .getClientResponsePayload()
                        .getDecisionElements()
                        .get(0)
                        .getRules();
        Rules.get(0).setRuleId(Ucode[0]);

        if (Ucode.length > 1) {
            for (int i = 1; i < Rules.size(); i++) {
                Rule newRule = new Rule();
                newRule.setRuleId(Ucode[i]);

                Rules.add(newRule);
            }
        }
    }
}
