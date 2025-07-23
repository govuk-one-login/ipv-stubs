package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.PostalAddress;

import java.util.List;

public class CriStubDataAddress {

    private CriStubDataAddress() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data = List.of(
            createData("Alice Doe Valid Address", Addresses.ALICE_DOE_ADDRESS),
            createData("Alice Parker Valid Address", Addresses.ALICE_PARKER_ADDRESS),
            createData("Alice Parker (Changed Address) Valid Address", Addresses.ALICE_PARKER_OLD_ADDRESS, Addresses.ALICE_PARKER_NEW_ADDRESS),
            createData("Bob Parker Valid Address", Addresses.BOB_PARKER_ADDRESS),
            createData("Claire Aarts (DWP)", Addresses.CLAIRE_AARTS_ADDRESS),
            createData("James Moriarty (Invalid) Address", Addresses.JAMES_MORIARTY_INVALID_ADDRESS),
            createData("Joe Shmoe Valid Address", Addresses.JOE_SCHMOE_ADDRESS),
            createData("Kabir Singh (DWP)", Addresses.KABIR_SINGH_ADDRESS),
            createData("Kenneth Decerqueira (Valid Experian) Address", Addresses.KENNETH_DECERQUERIA_ADDRESS),
            createData("Kenneth Decerqueira (Changed Address) Address", Addresses.KENNETH_DECERQUERIA_OLD_ADDRESS, Addresses.KENNETH_DECERQUERIA_NEW_ADDRESS),
            createData("Kenneth Decerqueira (International) Address", Addresses.KENNETH_DECERQUERIA_JAPAN),
            createData("Mary Watson Valid Address", Addresses.MARY_WATSON_ADDRESS),
            createData("Nora Porter (DWP)", Addresses.NORA_PORTER_ADDRESS),
            createData("Saul Goodman Valid Address", Addresses.SAUL_GOODMAN_ADDRESS),
            createData("Tom Hardy (DWP)", Addresses.TOM_HARDY_ADDRESS)
    );

    private static IdentityCheckSubjectCriStubData createData(String label, PostalAddress address) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(IdentityCheckSubject.builder()
                        .withAddress(List.of(address))
                        .build()).build();
    }

    private static IdentityCheckSubjectCriStubData createData(String label, PostalAddress address1, PostalAddress address2) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(IdentityCheckSubject.builder()
                        .withAddress(List.of(address1, address2))
                        .build()).build();
    }
}

