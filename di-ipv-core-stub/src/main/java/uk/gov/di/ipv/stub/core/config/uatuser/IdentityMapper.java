package uk.gov.di.ipv.stub.core.config.uatuser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class IdentityMapper {

    public static final String GIVEN_NAME = "GivenName";
    public static final String FAMILY_NAME = "FamilyName";

    public Identity map(Map<String, String> map, int rowNumber) {
        List<Question> listOfQuestions =
                map.keySet().stream()
                        .filter(k -> k.toLowerCase().startsWith("q0"))
                        .map(k -> new Question(k, new Answer(map.get(k))))
                        .collect(toList());

        String noOfQuestionsAfterApplyingBusinessRules =
                map.get("noOfQuestionsAfterApplyingBusinessRules");

        String noOfQuestionsTotal = map.get("noOfQuestionsTotal");
        int numAfterBus =
                noOfQuestionsAfterApplyingBusinessRules == null
                        ? 0
                        : Integer.parseInt(noOfQuestionsAfterApplyingBusinessRules);
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

        UKAddress address =
                new UKAddress(
                        houseNo,
                        map.get("street"),
                        map.get("district"),
                        map.get("posttown"),
                        map.get("postcode"),
                        true);

        Instant dob = Instant.parse(map.get("dob"));
        Instant dateOfEntryOnCtdb = Instant.parse(map.get("dateOfEntryOnCtdb"));
        FindDateOfBirth dateOfBirth = new FindDateOfBirth(dob, dateOfEntryOnCtdb);

        FullName name = new FullName(map.get("name"), map.get("surname"));

        return new Identity(
                rowNumber,
                map.get("accountNumber"),
                map.get("ctdbDatabase"),
                address,
                dateOfBirth,
                name,
                questions);
    }

    public DisplayIdentity mapToDisplayable(Identity identity) {
        return new DisplayIdentity(
                identity.rowNumber(),
                identity.name().firstLastName(),
                identity.questions().numQuestionsTotal());
    }

    public SharedClaims mapToSharedClaim(Identity identity) {
        return new SharedClaims(
                List.of(
                        "https://www.w3.org/2018/credentials/v1",
                        "https://vocab.london.cloudapps.digital/contexts/identity-v1.jsonld"),
                List.of(
                        new Name(
                                List.of(
                                        new NameParts(GIVEN_NAME, identity.name().firstName()),
                                        new NameParts(FAMILY_NAME, identity.name().surname())))),
                List.of(new DateOfBirth(identity.findDateOfBirth().getDOB())));
    }
}
