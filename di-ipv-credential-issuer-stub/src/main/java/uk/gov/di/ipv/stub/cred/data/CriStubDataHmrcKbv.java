package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;
import uk.gov.di.model.PostalAddress;

import java.util.List;

public class CriStubDataHmrcKbv {

    private CriStubDataHmrcKbv() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data = List.of(
            createData("Alice Parker (Valid) HMRC KBV", Names.Alice_Jane_Parker, BirthDates.ALICE_PARKER, Addresses.ALICE_PARKER_ADDRESS),
            createData("Bob Parker (Valid) HMRC KBV", Names.Bob_Parker, BirthDates.BOB_PARKER, Addresses.BOB_PARKER_ADDRESS),
            createData("James Moriarty (Invalid) HMRC KBV", Names.James_Moriarty, BirthDates.JAMES_MORIARTY, Addresses.JAMES_MORIARTY_INVALID_ADDRESS),
            createData("Kenneth Decerqueira (Valid Experian) HMRC KBV", Names.Kenneth_Decerqueira, BirthDates.KENNETH_DECERQUEIRA, Addresses.KENNETH_DECERQUERIA_ADDRESS),
            createData("Mary Watson (Valid) HMRC KBV", Names.Mary_Watson, BirthDates.MARY_WATSON, Addresses.MARY_WATSON_ADDRESS)
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

