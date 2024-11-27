package uk.gov.di.ipv.stub.core.config.uatuser;

import io.javalin.http.Context;
import uk.gov.di.ipv.stub.core.utils.StringHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                        map.get("postTown"),
                        map.get("postcode"),
                        addressValidFrom,
                        null);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(map.get("dob"), formatter);
        LocalDateTime dateTime = date.atStartOfDay();
        Instant dob = dateTime.toInstant(ZoneOffset.UTC);

        FindDateOfBirth dateOfBirth = new FindDateOfBirth(dob, dob);

        FullName name = new FullName(map.get("name"), map.get("initials"), map.get("surname"));
        String nino = null;

        return new Identity(
                rowNumber,
                map.get("accountNumber"),
                map.get("ctdbDatabase"),
                List.of(address),
                dateOfBirth,
                name,
                questions,
                nino);
    }

    public DisplayIdentity mapToDisplayable(Identity identity) {
        return new DisplayIdentity(
                identity.rowNumber(),
                identity.name().fullName(),
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

        List<NameParts> parts = new ArrayList<>();
        parts.add(new NameParts(GIVEN_NAME, identity.name().firstName()));
        if (identity.name().middleName() != null && !identity.name().middleName().isBlank()) {
            parts.add(new NameParts(GIVEN_NAME, identity.name().middleName()));
        }
        parts.add(new NameParts(FAMILY_NAME, identity.name().surname()));

        return new SharedClaims(
                List.of(
                        "https://www.w3.org/2018/credentials/v1",
                        "https://vocab.london.cloudapps.digital/contexts/identity-v1.jsonld"),
                List.of(new Name(parts)),
                List.of(new DateOfBirth(agedDOB ? dateOfBirth.getAgedDOB() : dateOfBirth.getDOB())),
                canonicalAddresses,
                identity.nino() == null ? null : List.of(new SocialSecurityRecord(identity.nino())),
                null);
    }

    public PostcodeSharedClaims mapToAddressSharedClaims(String postcode) {
        CanonicalAddress canonicalAddress =
                new CanonicalAddress(null, null, null, null, postcode, null, null);

        return new PostcodeSharedClaims(
                List.of(
                        "https://www.w3.org/2018/credentials/v1",
                        "https://vocab.london.cloudapps.digital/contexts/identity-v1.jsonld"),
                List.of(canonicalAddress));
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

    public Identity mapFormToIdentity(Identity identityOnRecord, Context ctx) {
        List<UKAddress> addresses = new ArrayList<>();

        LocalDate primaryAddressValidFrom =
                getLocalDate(ctx, "validFromYear", "validFromMonth", "validFromDay");

        LocalDate primaryAddressValidUntil =
                getLocalDate(ctx, "validUntilYear", "validUntilMonth", "validUntilDay");

        UKAddress primaryAddress =
                new UKAddress(
                        ctx.queryParam("buildingNumber"),
                        ctx.queryParam("buildingName"),
                        ctx.queryParam("street"),
                        null,
                        ctx.queryParam("townCity"),
                        ctx.queryParam("postCode"),
                        primaryAddressValidFrom,
                        primaryAddressValidUntil);
        addresses.add(primaryAddress);

        LocalDate secondaryAddressValidFrom =
                getLocalDate(
                        ctx,
                        "SecondaryUKAddress.validFromYear",
                        "SecondaryUKAddress.validFromMonth",
                        "SecondaryUKAddress.validFromDay");

        LocalDate secondaryAddressValidUntil =
                getLocalDate(
                        ctx,
                        "SecondaryUKAddress.validUntilYear",
                        "SecondaryUKAddress.validUntilMonth",
                        "SecondaryUKAddress.validUntilDay");

        UKAddress secondaryAddress =
                new UKAddress(
                        ctx.queryParam("SecondaryUKAddress.buildingNumber"),
                        ctx.queryParam("SecondaryUKAddress.buildingName"),
                        ctx.queryParam("SecondaryUKAddress.street"),
                        null,
                        ctx.queryParam("SecondaryUKAddress.townCity"),
                        ctx.queryParam("SecondaryUKAddress.postCode"),
                        secondaryAddressValidFrom,
                        secondaryAddressValidUntil);
        if (!Stream.of(
                        secondaryAddress.street(),
                        secondaryAddress.buildingName(),
                        secondaryAddress.buildingNumber(),
                        secondaryAddress.postCode(),
                        secondaryAddress.townCity(),
                        secondaryAddress.county(),
                        secondaryAddress.validFrom() != null
                                ? secondaryAddress.validFrom().toString()
                                : null,
                        secondaryAddress.validUntil() != null
                                ? secondaryAddress.validUntil().toString()
                                : null)
                .allMatch(StringHelper::isBlank)) {
            addresses.add(secondaryAddress);
        }

        LocalDate dob =
                getLocalDate(ctx, "dateOfBirth-year", "dateOfBirth-month", "dateOfBirth-day");
        Instant instant = dob.atStartOfDay(ZoneId.systemDefault()).toInstant();
        FindDateOfBirth findDateOfBirth =
                new FindDateOfBirth(
                        instant, LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());

        String enteredMiddleName = ctx.queryParam("middleName");
        String middleName = enteredMiddleName == null ? "" : enteredMiddleName;

        FullName fullName =
                new FullName(ctx.queryParam("firstName"), middleName, ctx.queryParam("surname"));
        String nino = ctx.queryParam("nationalInsuranceNumber");
        return new Identity(
                identityOnRecord.rowNumber(),
                identityOnRecord.accountNumber(),
                identityOnRecord.ctdbDatabase(),
                addresses,
                findDateOfBirth,
                fullName,
                identityOnRecord.questions(),
                nino);
    }

    private LocalDate getLocalDate(Context ctx, String year, String month, String day) {
        if (!Stream.of(ctx.queryParam(year), ctx.queryParam(month), ctx.queryParam(day))
                .allMatch(StringHelper::isBlank)) {

            return LocalDate.of(
                    Integer.parseInt(ctx.queryParam(year)),
                    Integer.parseInt(ctx.queryParam(month)),
                    Integer.parseInt(ctx.queryParam(day)));
        }
        return null;
    }
}
