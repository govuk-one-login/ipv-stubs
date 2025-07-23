package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;

import java.util.List;

public class CriStubDataClaimedIdentity {

    private CriStubDataClaimedIdentity() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data = List.of(
            createData("Alice Doe", Names.Alice_Jane_Laura_Doe, BirthDates.ALICE_DOE),
            createData("Alice Parker", Names.Alice_Jane_Parker, BirthDates.ALICE_PARKER),
            createData("Bob Parker", Names.Bob_Parker, BirthDates.BOB_PARKER),
            createData("Claire Aarts (DWP)", Names.Claire_Aarts, BirthDates.CLAIRE_AARTS_DWP),
            createData("James Moriarty", Names.James_Moriarty, BirthDates.JAMES_MORIARTY),
            createData("Joe Shmoe", Names.Joe_Schmoe, BirthDates.JOE_SHMOE),
            createData("Kabir Singh (DWP)", Names.Kabir_Singh, BirthDates.KABIR_SINGH_DWP),
            createData("Kenneth Decerqueira", Names.Kenneth_Decerqueira, BirthDates.KENNETH_DECERQUEIRA),
            createData("Mary Watson", Names.Mary_Watson, BirthDates.MARY_WATSON),
            createData("Nora Porter (DWP)", Names.Nora_Porter, BirthDates.NORA_PORTER_DWP),
            createData("Saul Goodman", Names.SAUL_GOODMAN, BirthDates.SAUL_GOODMAN),
            createData("Tom Hardy (DWP)", Names.Tom_Hardy, BirthDates.TOM_HARDY_DWP)
    );

    private static IdentityCheckSubjectCriStubData createData(String label, Name name, BirthDate birthDate) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(IdentityCheckSubject.builder()
                        .withName(List.of(name))
                        .withBirthDate(List.of(birthDate))
                        .build()).build();
    }
}

