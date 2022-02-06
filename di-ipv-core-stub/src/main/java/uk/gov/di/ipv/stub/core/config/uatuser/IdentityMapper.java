package uk.gov.di.ipv.stub.core.config.uatuser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class IdentityMapper {

    public Identity map(Map<String, String> map, int rowNumber) {
        List<Question> listOfQuestions = map.keySet().stream()
                .filter(k -> k.toLowerCase().startsWith("q0"))
                .map(k -> new Question(k, new Answer(map.get(k))))
                .collect(toList());

        String noOfQuestionsAfterApplyingBusinessRules = map.get("noOfQuestionsAfterApplyingBusinessRules");

        String noOfQuestionsTotal = map.get("noOfQuestionsTotal");
        int numAfterBus = noOfQuestionsAfterApplyingBusinessRules == null ? 0 : Integer.parseInt(noOfQuestionsAfterApplyingBusinessRules);
        int numTotal = noOfQuestionsTotal == null ? 0 : Integer.parseInt(noOfQuestionsTotal);


        Questions questions = new Questions(listOfQuestions, numAfterBus, numTotal);


        Object hn = map.get("houseNo");
        String houseNo;
        if (hn instanceof Double d) {
            int i = (int) Math.round(d);
            houseNo = String.valueOf(i);
        } else {
            houseNo = (String) hn;
        }

        UKAddress address = new UKAddress(houseNo, map.get("street"), map.get("district"), map.get("posttown"), map.get("postcode"), true);

        Instant dob = Instant.parse(map.get("dob"));
        Instant dateOfEntryOnCtdb = Instant.parse(map.get("dateOfEntryOnCtdb"));
        DateOfBirth dateOfBirth = new DateOfBirth(dob, dateOfEntryOnCtdb);

        Name name = new Name(map.get("name"), map.get("surname"));

        return new Identity(rowNumber, map.get("accountNumber"), map.get("ctdbDatabase"), address, dateOfBirth, name, questions);

    }

    public DisplayIdentity mapToDisplayable(Identity identity) {
        return new DisplayIdentity(identity.rowNumber(), identity.name().firstLastName(), identity.questions().numQuestionsTotal());
    }

    public JWTClaimIdentity mapToJTWClaim(Identity identity) {
        return new JWTClaimIdentity(
                List.of(identity.name()),
                List.of(identity.UKAddress()),
                List.of(identity.dateOfBirth().getDOB()));
    }
}
