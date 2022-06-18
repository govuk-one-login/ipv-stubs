package uk.gov.di.ipv.stub.core.config.uatuser;

import spark.QueryParamsMap;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
                        map.get("houseName"),
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
                List.of(new DateOfBirth(identity.findDateOfBirth().getDOB())),
                List.of(
                        new CanonicalAddress(
                                identity.UKAddress().buildingNumber(),
                                identity.UKAddress().buildingName(),
                                identity.UKAddress().street(),
                                identity.UKAddress().townCity(),
                                identity.UKAddress().postCode(),
                                // default / arbitrary value assigned for now as
                                // the validFrom date is not available in test data
                                LocalDate.of(2021, 1, 1),
                                null)));
    }

    public List<QuestionAndAnswer> mapToQuestionAnswers(
            Identity identity, Map<String, String> questionsMap) {
        return identity.questions().questions().stream()
                .map(
                        q ->
                                new QuestionAndAnswer(
                                        q.questionId().toUpperCase(),
                                        questionsMap.get(q.questionId().toUpperCase()),
                                        q.answer().answer()))
                .collect(toList());
    }

    public Identity mapFormToIdentity(Identity identityOnRecord, QueryParamsMap userData) {

        UKAddress ukAddress =
                new UKAddress(
                        userData.value("buildingNumber"),
                        userData.value("buildingName"),
                        userData.value("street"),
                        null,
                        userData.value("townCity"),
                        userData.value("postCode"),
                        true);

        String dobString =
                userData.value("dateOfBirth-year")
                        + "-"
                        + userData.value("dateOfBirth-month")
                        + "-"
                        + userData.value("dateOfBirth-day");
        LocalDate dob = LocalDate.parse(dobString);
        Instant instant = dob.atStartOfDay(ZoneId.systemDefault()).toInstant();
        FindDateOfBirth findDateOfBirth = new FindDateOfBirth(instant, instant);
        FullName fullName = new FullName(userData.value("firstName"), userData.value("surname"));
        return new Identity(
                identityOnRecord.rowNumber(),
                identityOnRecord.accountNumber(),
                identityOnRecord.ctdbDatabase(),
                ukAddress,
                findDateOfBirth,
                fullName,
                identityOnRecord.questions());
    }
}
