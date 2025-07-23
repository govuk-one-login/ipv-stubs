package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;
import uk.gov.di.model.PostalAddress;

import java.util.List;

public class CriStubDataExperianKbv {

    private CriStubDataExperianKbv() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data = List.of(
            createData("Alice Parker (Valid) KBV", Names.Alice_Jane_Parker, BirthDates.ALICE_PARKER, Addresses.ALICE_PARKER_ADDRESS),
            createData("Bob Parker (Valid) KBV", Names.Bob_Parker, BirthDates.BOB_PARKER, Addresses.BOB_PARKER_ADDRESS),
            createData("Claire Aarts KBV (DWP)", Names.Claire_Aarts, BirthDates.CLAIRE_AARTS_DWP, Addresses.CLAIRE_AARTS_ADDRESS),
            createData("James Moriarty (Invalid) KBV", Names.James_Moriarty, BirthDates.JAMES_MORIARTY, Addresses.JAMES_MORIARTY_INVALID_ADDRESS),
            createData("Kabir Singh KBV (DWP)", Names.Kabir_Singh, BirthDates.KABIR_SINGH_DWP, Addresses.KABIR_SINGH_ADDRESS),
            createData("Kenneth Decerqueira (Valid Experian) KBV", Names.Kenneth_Decerqueira, BirthDates.KENNETH_DECERQUEIRA, Addresses.KENNETH_DECERQUERIA_ADDRESS),
            createData("Nora Porter KBV (DWP)", Names.Nora_Porter, BirthDates.NORA_PORTER_DWP, Addresses.NORA_PORTER_ADDRESS),
            createData("Mary Watson (Valid) KBV", Names.Mary_Watson, BirthDates.MARY_WATSON, Addresses.MARY_WATSON_ADDRESS),
            createData("Tom Hardy KBV (DWP)", Names.Tom_Hardy, BirthDates.TOM_HARDY_DWP, Addresses.TOM_HARDY_ADDRESS)
    );

    private static IdentityCheckSubjectCriStubData createData(String label, Name name, BirthDate birthDate, PostalAddress address) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(IdentityCheckSubject.builder()
                        .withName(List.of(name))
                        .withBirthDate(List.of(birthDate))
                        .withAddress(List.of(address))
                        .build()).build();
    }
}

