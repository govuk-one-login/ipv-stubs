package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;
import uk.gov.di.model.SocialSecurityRecordDetails;

import java.util.List;

public class CriStubDataNino {

    private CriStubDataNino() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data =
            List.of(
                    createData(
                            "Alice Parker (Valid) National Insurance",
                            Names.Alice_Jane_Parker,
                            BirthDates.ALICE_PARKER,
                            Ninos.NINO_VALID),
                    createData(
                            "Bob Parker (Valid) National Insurance",
                            Names.Bob_Parker,
                            BirthDates.BOB_PARKER,
                            Ninos.NINO_VALID),
                    createData(
                            "Claire Aarts National Insurance (DWP)",
                            Names.Claire_Aarts,
                            BirthDates.CLAIRE_AARTS_DWP,
                            Ninos.NINO_CLAIRE_AARTS),
                    createData(
                            "Kabir Singh National Insurance (DWP)",
                            Names.Kabir_Singh,
                            BirthDates.KABIR_SINGH_DWP,
                            Ninos.NINO_KABIR_SINGH),
                    createData(
                            "Kenneth Decerqueira (Valid) National Insurance",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Ninos.NINO_VALID),
                    createData(
                            "Nora Porter National Insurance (DWP)",
                            Names.Nora_Porter,
                            BirthDates.NORA_PORTER_DWP,
                            Ninos.NINO_NORA_PORTER),
                    createData(
                            "Mary Watson (Valid) National Insurance",
                            Names.Mary_Watson,
                            BirthDates.MARY_WATSON,
                            Ninos.NINO_VALID),
                    createData(
                            "Tom Hardy National Insurance (DWP)",
                            Names.Tom_Hardy,
                            BirthDates.TOM_HARDY_DWP,
                            Ninos.NINO_TOM_HARDY));

    private static IdentityCheckSubjectCriStubData createData(
            String label, Name name, BirthDate birthDate, SocialSecurityRecordDetails nino) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(
                        IdentityCheckSubject.builder()
                                .withName(List.of(name))
                                .withBirthDate(List.of(birthDate))
                                .withSocialSecurityRecord(List.of(nino))
                                .build())
                .build();
    }
}
