package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.Name;
import uk.gov.di.model.NamePart;

import java.util.ArrayList;

public class Names {

    public static final Name ALICE_JANE_LAURA_DOE = createName("ALICE", "JANE", "LAURA", "DOE");
    public static final Name Alice_Jane_Laura_Doe = createName("Alice", "Jane", "Laura", "Doe");
    public static final Name ALICE_JANE_SMITH = createName("ALICE", "JANE", "SMITH");
    public static final Name Alice_Jane_Smith = createName("Alice", "Jane", "Smith");
    public static final Name ALICE_JANE_PARKER = createName("ALICE", "JANE", "PARKER");
    public static final Name Alice_Jane_Parker = createName("Alice", "Jane", "Parker");
    public static final Name ALISON_JANE_PARKER = createName("ALISON", "JANE", "PARKER");
    public static final Name Alison_Jane_Parker = createName("Alison", "Jane", "Parker");
    public static final Name Billy_Batson = createName("Billy", "Batson");
    public static final Name Bob_Parker = createName("Bob", "Parker");
    public static final Name CLAIRE_AARTS = createName("CLAIRE", "AARTS");
    public static final Name Claire_Aarts = createName("Claire", "Aarts");
    public static final Name James_Moriarty = createName("James", "Moriarty");
    public static final Name Joe_Schmoe = createName("Joe", "Schmoe");
    public static final Name John_Roberts = createName("John", "Roberts");
    public static final Name KABIR_SINGH = createName("KABIR", "SINGH");
    public static final Name Kabir_Singh = createName("Kabir", "Singh");
    public static final Name KENNETH_DECERQUEIRA = createName("KENNETH", "DECERQUEIRA");
    public static final Name Kenneth_Decerqueira = createName("Kenneth", "Decerqueira");
    public static final Name KENNETH_JONES = createName("KENNETH", "JONES");
    public static final Name Kenneth_Jones = createName("Kenneth", "Jones");
    public static final Name MICHAEL_DECERQUEIRA = createName("MICHAEL", "DECERQUEIRA");
    public static final Name Michael_Decerqueira = createName("Michael", "Decerqueira");
    public static final Name Mary_Watson = createName("Mary", "Watson");
    public static final Name NORA_PORTER = createName("NORA", "PORTER");
    public static final Name Nora_Porter = createName("Nora", "Porter");
    public static final Name SAUL_GOODMAN = createName("SAUL", "GOODMAN");
    public static final Name Saul_Goodman = createName("Saul", "Goodman");
    public static final Name TOM_HARDY = createName("TOM", "HARDY");
    public static final Name Tom_Hardy = createName("Tom", "Hardy");

    private Names() {
        // Replace default public constructor
    }

    private static Name createName(String firstName, String lastName) {
        var nameParts = new ArrayList<NamePart>();
        nameParts.add(
                NamePart.builder()
                        .withType(NamePart.NamePartType.GIVEN_NAME)
                        .withValue(firstName)
                        .build());
        nameParts.add(
                NamePart.builder()
                        .withType(NamePart.NamePartType.FAMILY_NAME)
                        .withValue(lastName)
                        .build());

        return Name.builder().withNameParts(nameParts).build();
    }

    private static Name createName(String firstName, String middleName, String lastName) {
        var nameParts = new ArrayList<NamePart>();
        nameParts.add(
                NamePart.builder()
                        .withType(NamePart.NamePartType.GIVEN_NAME)
                        .withValue(firstName)
                        .build());
        nameParts.add(
                NamePart.builder()
                        .withType(NamePart.NamePartType.GIVEN_NAME)
                        .withValue(middleName)
                        .build());
        nameParts.add(
                NamePart.builder()
                        .withType(NamePart.NamePartType.FAMILY_NAME)
                        .withValue(lastName)
                        .build());

        return Name.builder().withNameParts(nameParts).build();
    }

    private static Name createName(
            String firstName, String middleName, String secondMiddleName, String lastName) {
        var nameParts = new ArrayList<NamePart>();
        nameParts.add(
                NamePart.builder()
                        .withType(NamePart.NamePartType.GIVEN_NAME)
                        .withValue(firstName)
                        .build());
        nameParts.add(
                NamePart.builder()
                        .withType(NamePart.NamePartType.GIVEN_NAME)
                        .withValue(middleName)
                        .build());
        nameParts.add(
                NamePart.builder()
                        .withType(NamePart.NamePartType.GIVEN_NAME)
                        .withValue(secondMiddleName)
                        .build());
        nameParts.add(
                NamePart.builder()
                        .withType(NamePart.NamePartType.FAMILY_NAME)
                        .withValue(lastName)
                        .build());

        return Name.builder().withNameParts(nameParts).build();
    }
}
