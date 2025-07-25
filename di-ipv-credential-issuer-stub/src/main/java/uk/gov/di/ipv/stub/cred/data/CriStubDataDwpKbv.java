package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;
import uk.gov.di.model.PostalAddress;

import java.util.List;

public class CriStubDataDwpKbv {

    private CriStubDataDwpKbv() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data =
            List.of(
                    createData(
                            "Alice Parker (Valid) DWP KBV",
                            Names.Alice_Jane_Parker,
                            BirthDates.ALICE_PARKER,
                            Addresses.ALICE_PARKER_ADDRESS),
                    createData(
                            "Bob Parker (Valid) DWP KBV",
                            Names.Bob_Parker,
                            BirthDates.BOB_PARKER,
                            Addresses.BOB_PARKER_ADDRESS),
                    createData(
                            "Claire Aarts (Valid) DWP KBV (Thin file)",
                            Names.Claire_Aarts,
                            BirthDates.CLAIRE_AARTS,
                            Addresses.CLAIRE_AARTS_ADDRESS),
                    createData(
                            "James Moriarty (Invalid) DWP KBV",
                            Names.James_Moriarty,
                            BirthDates.JAMES_MORIARTY,
                            Addresses.JAMES_MORIARTY_INVALID_ADDRESS),
                    createData(
                            "Kenneth Decerqueira (Valid Experian) DWP KBV",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Addresses.KENNETH_DECERQUERIA_ADDRESS),
                    createData(
                            "Mary Watson (Valid) DWP KBV",
                            Names.Mary_Watson,
                            BirthDates.MARY_WATSON,
                            Addresses.MARY_WATSON_ADDRESS),
                    createData(
                            "Tom Hardy (Valid) DWP KBV",
                            Names.Tom_Hardy,
                            BirthDates.TOM_HARDY,
                            Addresses.TOM_HARDY_ADDRESS));

    private static IdentityCheckSubjectCriStubData createData(
            String label, Name name, BirthDate birthDate, PostalAddress address) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(
                        IdentityCheckSubject.builder()
                                .withName(List.of(name))
                                .withBirthDate(List.of(birthDate))
                                .withAddress(List.of(address))
                                .build())
                .build();
    }
}
