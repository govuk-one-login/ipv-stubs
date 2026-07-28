package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;

import java.util.List;

public class CriStubDataOpenBanking {

    private CriStubDataOpenBanking() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data =
            List.of(
                    createData(
                            "Alice Parker",
                            Names.Alice_Jane_Parker,
                            BirthDates.ALICE_PARKER),
                    createData(
                            "Bob Parker",
                            Names.Bob_Parker,
                            BirthDates.BOB_PARKER),
                    createData(
                            "James Moriarty",
                            Names.James_Moriarty,
                            BirthDates.JAMES_MORIARTY),
                    createData(
                            "Kenneth Decerqueira",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA),
                    createData(
                            "Mary Watson",
                            Names.Mary_Watson,
                            BirthDates.MARY_WATSON));

    private static IdentityCheckSubjectCriStubData createData(
            String label, Name name, BirthDate birthDate) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(
                        IdentityCheckSubject.builder()
                                .withName(List.of(name))
                                .withBirthDate(List.of(birthDate))
                                .build())
                .build();
    }
}
