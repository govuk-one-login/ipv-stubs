package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.Name;
import uk.gov.di.model.NamePart;

import java.util.ArrayList;

public class Names {

    public final static Name ALICE_JANE_LAURA_DOE = createName("ALICE", "JANE", "LAURA", "DOE");
    public final static Name Alice_Jane_Laura_Doe = createName("Alice", "Jane", "Laura", "Doe");
    public final static Name ALICE_JANE_SMITH = createName("ALICE", "JANE", "SMITH");
    public final static Name Alice_Jane_Smith = createName("Alice", "Jane", "Smith");
    public final static Name ALICE_JANE_PARKER = createName("ALICE", "JANE", "PARKER");
    public final static Name Alice_Jane_Parker = createName("Alice", "Jane", "Parker");
    public final static Name ALISON_JANE_PARKER = createName("ALISON", "JANE", "PARKER");
    public final static Name Alison_Jane_Parker = createName("Alison", "Jane", "Parker");
    public final static Name Billy_Batson = createName("Billy", "Batson");
    public final static Name Bob_Parker = createName("Bob", "Parker");
    public final static Name CLAIRE_AARTS = createName("CLAIRE", "AARTS");
    public final static Name Claire_Aarts = createName("Claire", "Aarts");
    public final static Name James_Moriarty = createName("James", "Moriarty");
    public final static Name Joe_Schmoe = createName("Joe", "Schmoe");
    public final static Name John_Roberts = createName("John", "Roberts");
    public final static Name KABIR_SINGH = createName("KABIR", "SINGH");
    public final static Name Kabir_Singh = createName("Kabir", "Singh");
    public final static Name KENNETH_DECERQUEIRA = createName("KENNETH", "DECERQUEIRA");
    public final static Name Kenneth_Decerqueira = createName("Kenneth", "Decerqueira");
    public final static Name KENNETH_JONES = createName("KENNETH", "JONES");
    public final static Name Kenneth_Jones = createName("Kenneth", "Jones");
    public final static Name MICHAEL_DECERQUEIRA = createName("MICHAEL", "DECERQUEIRA");
    public final static Name Michael_Decerqueira = createName("Michael", "Decerqueira");
    public final static Name Mary_Watson = createName("Mary", "Watson");
    public final static Name NORA_PORTER = createName("NORA", "PORTER");
    public final static Name Nora_Porter = createName("Nora", "Porter");
    public final static Name SAUL_GOODMAN = createName("SAUL", "GOODMAN");
    public final static Name Saul_Goodman = createName("Saul", "Goodman");
    public final static Name TOM_HARDY = createName("TOM", "HARDY");
    public final static Name Tom_Hardy = createName("Tom", "Hardy");

    private Names() {
        // Replace default public constructor
    }

    private static Name createName(String firstName, String lastName) {
        var nameParts = new ArrayList<NamePart>();
        nameParts.add(NamePart.builder().withType(NamePart.NamePartType.GIVEN_NAME).withValue(firstName).build());
        nameParts.add(NamePart.builder().withType(NamePart.NamePartType.FAMILY_NAME).withValue(lastName).build());

        return Name.builder().withNameParts(nameParts).build();
    }

    private static Name createName(String firstName, String middleName, String lastName) {
        var nameParts = new ArrayList<NamePart>();
        nameParts.add(NamePart.builder().withType(NamePart.NamePartType.GIVEN_NAME).withValue(firstName).build());
        nameParts.add(NamePart.builder().withType(NamePart.NamePartType.GIVEN_NAME).withValue(middleName).build());
        nameParts.add(NamePart.builder().withType(NamePart.NamePartType.FAMILY_NAME).withValue(lastName).build());

        return Name.builder().withNameParts(nameParts).build();
    }

    private static Name createName(String firstName, String middleName, String secondMiddleName, String lastName) {
        var nameParts = new ArrayList<NamePart>();
        nameParts.add(NamePart.builder().withType(NamePart.NamePartType.GIVEN_NAME).withValue(firstName).build());
        nameParts.add(NamePart.builder().withType(NamePart.NamePartType.GIVEN_NAME).withValue(middleName).build());
        nameParts.add(NamePart.builder().withType(NamePart.NamePartType.GIVEN_NAME).withValue(secondMiddleName).build());
        nameParts.add(NamePart.builder().withType(NamePart.NamePartType.FAMILY_NAME).withValue(lastName).build());

        return Name.builder().withNameParts(nameParts).build();
    }
}
