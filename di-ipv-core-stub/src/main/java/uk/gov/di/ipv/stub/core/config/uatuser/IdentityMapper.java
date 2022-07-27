package uk.gov.di.ipv.stub.core.config.uatuser;

import spark.QueryParamsMap;
import spark.utils.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

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

        LocalDate addressValidFrom = LocalDate.of(2021, 1, 1);
        UKAddress address =
                new UKAddress(
                        houseNo,
                        map.get("houseName"),
                        map.get("street"),
                        map.get("district"),
                        map.get("posttown"),
                        map.get("postcode"),
                        addressValidFrom,
                        null);

        Instant dob = Instant.parse(map.get("dob"));
        Instant dateOfEntryOnCtdb = Instant.parse(map.get("dateOfEntryOnCtdb"));
        FindDateOfBirth dateOfBirth = new FindDateOfBirth(dob, dateOfEntryOnCtdb);

        FullName name = new FullName(map.get("name"), map.get("surname"));

        return new Identity(
                rowNumber,
                map.get("accountNumber"),
                map.get("ctdbDatabase"),
                List.of(address),
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

    public SharedClaims mapToSharedClaim(Identity identity, boolean agedDOB) {
        FindDateOfBirth dateOfBirth = identity.findDateOfBirth();

        AtomicInteger mapIteration = new AtomicInteger();
        List<CanonicalAddress> canonicalAddresses =
                identity.addresses().stream()
                        .map(
                                address -> {
                                    LocalDate validFrom = address.validFrom();
                                    if (mapIteration.get() == 0) {
                                        validFrom =
                                                address.validFrom() != null
                                                        ? address.validFrom()
                                                        : LocalDate.of(2021, 1, 1);
                                    }
                                    mapIteration.getAndIncrement();
                                    return new CanonicalAddress(
                                            address.buildingNumber(),
                                            address.buildingName(),
                                            address.street(),
                                            address.townCity(),
                                            address.postCode(),
                                            // default / arbitrary value assigned for now as
                                            // the validFrom date is not available in test data
                                            validFrom,
                                            address.validUntil());
                                })
                        .collect(toList());

        return new SharedClaims(
                List.of(
                        "https://www.w3.org/2018/credentials/v1",
                        "https://vocab.london.cloudapps.digital/contexts/identity-v1.jsonld"),
                List.of(
                        new Name(
                                List.of(
                                        new NameParts(GIVEN_NAME, identity.name().firstName()),
                                        new NameParts(FAMILY_NAME, identity.name().surname())))),
                List.of(new DateOfBirth(agedDOB ? dateOfBirth.getAgedDOB() : dateOfBirth.getDOB())),
                canonicalAddresses);
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

    public Identity mapFormToIdentity(Identity identityOnRecord, QueryParamsMap formData) {
        List<UKAddress> addresses = new ArrayList<>();

        LocalDate primaryAddressValidFrom =
                getLocalDate(formData, "validFromYear", "validFromMonth", "validFromDay");

        LocalDate primaryAddressValidUntil =
                getLocalDate(formData, "validUntilYear", "validUntilMonth", "validUntilDay");

        UKAddress primaryAddress =
                new UKAddress(
                        formData.value("buildingNumber"),
                        formData.value("buildingName"),
                        formData.value("street"),
                        null,
                        formData.value("townCity"),
                        formData.value("postCode"),
                        primaryAddressValidFrom,
                        primaryAddressValidUntil);
        addresses.add(primaryAddress);

        LocalDate secondaryAddressValidFrom =
                getLocalDate(
                        formData,
                        "SecondaryUKAddress.validFromYear",
                        "SecondaryUKAddress.validFromMonth",
                        "SecondaryUKAddress.validFromDay");

        LocalDate secondaryAddressValidUntil =
                getLocalDate(
                        formData,
                        "SecondaryUKAddress.validUntilYear",
                        "SecondaryUKAddress.validUntilMonth",
                        "SecondaryUKAddress.validUntilDay");

        UKAddress secondaryAddress =
                new UKAddress(
                        formData.value("SecondaryUKAddress.buildingNumber"),
                        formData.value("SecondaryUKAddress.buildingName"),
                        formData.value("SecondaryUKAddress.street"),
                        null,
                        formData.value("SecondaryUKAddress.townCity"),
                        formData.value("SecondaryUKAddress.postCode"),
                        secondaryAddressValidFrom,
                        secondaryAddressValidUntil);
        if (!Stream.of(
                        secondaryAddress.street(),
                        secondaryAddress.buildingName(),
                        secondaryAddress.buildingNumber(),
                        secondaryAddress.postCode(),
                        secondaryAddress.townCity(),
                        secondaryAddress.county(),
                        secondaryAddress.validFrom(),
                        secondaryAddress.validFrom(),
                        secondaryAddress.validUntil())
                .allMatch(Objects::isNull)) {
            addresses.add(secondaryAddress);
        }

        LocalDate dob =
                getLocalDate(formData, "dateOfBirth-year", "dateOfBirth-month", "dateOfBirth-day");
        Instant instant = dob.atStartOfDay(ZoneId.systemDefault()).toInstant();
        FindDateOfBirth findDateOfBirth = new FindDateOfBirth(instant, instant);
        FullName fullName = new FullName(formData.value("firstName"), formData.value("surname"));
        return new Identity(
                identityOnRecord.rowNumber(),
                identityOnRecord.accountNumber(),
                identityOnRecord.ctdbDatabase(),
                addresses,
                findDateOfBirth,
                fullName,
                identityOnRecord.questions());
    }

    private LocalDate getLocalDate(QueryParamsMap userData, String year, String month, String day) {
        if (!Stream.of(userData.value(year), userData.value(month), userData.value(day))
                .allMatch(StringUtils::isBlank)) {

            return LocalDate.of(
                    Integer.parseInt(userData.value(year)),
                    Integer.parseInt(userData.value(month)),
                    Integer.parseInt(userData.value(day)));
        }
        return null;
    }
}
