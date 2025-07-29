package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;
import uk.gov.di.model.PassportDetails;

import java.util.List;

public class CriStubDataUkPassport {

    private CriStubDataUkPassport() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data =
            List.of(
                    createData(
                            "Alice Parker (Valid) Passport",
                            Names.Alice_Jane_Parker,
                            BirthDates.ALICE_PARKER,
                            Passports.ALICE_PARKER_PASSPORT),
                    createData(
                            "Claire Aarts Passport (DWP)",
                            Names.Claire_Aarts,
                            BirthDates.CLAIRE_AARTS,
                            Passports.CLAIRE_AARTS_PASSPORT),
                    createData(
                            "James Moriarty (Invalid) Passport",
                            Names.James_Moriarty,
                            BirthDates.JAMES_MORIARTY,
                            Passports.JAMES_MORIARTY_PASSPORT_INVALID),
                    createData(
                            "Kabir Singh Passport (DWP)",
                            Names.Kabir_Singh,
                            BirthDates.KABIR_SINGH,
                            Passports.KABIR_SINGH_PASSPORT),
                    createData(
                            "Kenneth Decerqueira (Valid Experian) Passport",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Passports.KENNETH_DECERQUEIRA_PASSPORT),
                    createData(
                            "Nora Porter Passport (DWP)",
                            Names.Nora_Porter,
                            BirthDates.NORA_PORTER,
                            Passports.NORA_PORTER_PASSPORT),
                    createData(
                            "Mary Watson (Valid) Passport",
                            Names.Mary_Watson,
                            BirthDates.MARY_WATSON,
                            Passports.MARY_WATSON_PASSPORT),
                    createData(
                            "Tom Hardy Passport (DWP)",
                            Names.Tom_Hardy,
                            BirthDates.TOM_HARDY,
                            Passports.TOM_HARDY_PASSPORT));

    private static IdentityCheckSubjectCriStubData createData(
            String label, Name name, BirthDate birthDate, PassportDetails passport) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(
                        IdentityCheckSubject.builder()
                                .withName(List.of(name))
                                .withBirthDate(List.of(birthDate))
                                .withPassport(List.of(passport))
                                .build())
                .build();
    }
}
