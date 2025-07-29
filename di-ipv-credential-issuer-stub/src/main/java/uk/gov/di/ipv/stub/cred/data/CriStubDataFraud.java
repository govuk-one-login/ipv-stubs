package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;
import uk.gov.di.model.PostalAddress;

import java.util.List;

public class CriStubDataFraud {

    private CriStubDataFraud() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data =
            List.of(
                    createData(
                            "Alice Doe (Valid) Fraud",
                            Names.Alice_Jane_Laura_Doe,
                            BirthDates.ALICE_DOE,
                            Addresses.ALICE_DOE_ADDRESS),
                    createData(
                            "Alice Parker (Valid) Fraud",
                            Names.Alice_Jane_Parker,
                            BirthDates.ALICE_PARKER,
                            Addresses.ALICE_PARKER_ADDRESS),
                    createData(
                            "Alice Parker (Changed Address) Fraud",
                            Names.Alice_Jane_Parker,
                            BirthDates.ALICE_PARKER,
                            Addresses.ALICE_PARKER_ADDRESS),
                    createData(
                            "Alice Parker (Changed First Name + Address) Fraud",
                            Names.Alison_Jane_Parker,
                            BirthDates.ALICE_PARKER,
                            Addresses.ALICE_PARKER_OLD_ADDRESS,
                            Addresses.ALICE_PARKER_NEW_ADDRESS),
                    createData(
                            "Alice Parker (Changed First Name) Fraud",
                            Names.Alison_Jane_Parker,
                            BirthDates.ALICE_PARKER,
                            Addresses.ALICE_PARKER_ADDRESS),
                    createData(
                            "Alice Parker (Changed Last Name + Address) Fraud",
                            Names.Alice_Jane_Smith,
                            BirthDates.ALICE_PARKER,
                            Addresses.ALICE_PARKER_ADDRESS),
                    createData(
                            "Alice Parker (Changed Last Name) Fraud",
                            Names.Alice_Jane_Smith,
                            BirthDates.ALICE_PARKER,
                            Addresses.ALICE_PARKER_ADDRESS),
                    createData(
                            "Bob Parker (Valid) Fraud",
                            Names.Bob_Parker,
                            BirthDates.BOB_PARKER,
                            Addresses.BOB_PARKER_ADDRESS),
                    createData(
                            "Claire Aarts Fraud (DWP)",
                            Names.Claire_Aarts,
                            BirthDates.CLAIRE_AARTS,
                            Addresses.CLAIRE_AARTS_ADDRESS),
                    createData(
                            "James Moriarty (Invalid) Fraud",
                            Names.James_Moriarty,
                            BirthDates.JAMES_MORIARTY,
                            Addresses.JAMES_MORIARTY_INVALID_ADDRESS),
                    createData(
                            "Joe Shmoe (Valid) Fraud",
                            Names.Joe_Schmoe,
                            BirthDates.JOE_SHMOE,
                            Addresses.JOE_SCHMOE_ADDRESS),
                    createData(
                            "Kabir Singh Fraud (DWP)",
                            Names.Kabir_Singh,
                            BirthDates.KABIR_SINGH,
                            Addresses.KABIR_SINGH_ADDRESS),
                    createData(
                            "Kenneth Decerqueira (Valid Experian) Fraud",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Addresses.KENNETH_DECERQUERIA_ADDRESS),
                    createData(
                            "Kenneth Decerqueira (Changed Address) Fraud",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Addresses.KENNETH_DECERQUERIA_OLD_ADDRESS,
                            Addresses.KENNETH_DECERQUERIA_NEW_ADDRESS),
                    createData(
                            "Kenneth Decerqueira (Changed First Name + Address) Fraud",
                            Names.Michael_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Addresses.KENNETH_DECERQUERIA_OLD_ADDRESS,
                            Addresses.KENNETH_DECERQUERIA_NEW_ADDRESS),
                    createData(
                            "Kenneth Decerqueira (Changed First Name) Fraud",
                            Names.Michael_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Addresses.KENNETH_DECERQUERIA_ADDRESS),
                    createData(
                            "Kenneth Decerqueira (Changed Last Name + Address) Fraud",
                            Names.Kenneth_Jones,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Addresses.KENNETH_DECERQUERIA_OLD_ADDRESS,
                            Addresses.KENNETH_DECERQUERIA_NEW_ADDRESS),
                    createData(
                            "Kenneth Decerqueira (Changed Last Name) Fraud",
                            Names.Kenneth_Jones,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Addresses.KENNETH_DECERQUERIA_ADDRESS),
                    createData(
                            "Kenneth Decerqueira (International) Fraud",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Addresses.KENNETH_DECERQUERIA_JAPAN),
                    createData(
                            "Nora Porter Fraud (DWP)",
                            Names.Nora_Porter,
                            BirthDates.NORA_PORTER,
                            Addresses.NORA_PORTER_ADDRESS),
                    createData(
                            "Mary Watson (Valid) Fraud",
                            Names.Mary_Watson,
                            BirthDates.MARY_WATSON,
                            Addresses.MARY_WATSON_ADDRESS),
                    createData(
                            "Saul Goodman (Valid) Fraud",
                            Names.SAUL_GOODMAN,
                            BirthDates.SAUL_GOODMAN,
                            Addresses.SAUL_GOODMAN_ADDRESS),
                    createData(
                            "Tom Hardy Fraud (DWP)",
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

    private static IdentityCheckSubjectCriStubData createData(
            String label,
            Name name,
            BirthDate birthDate,
            PostalAddress address1,
            PostalAddress address2) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(
                        IdentityCheckSubject.builder()
                                .withName(List.of(name))
                                .withBirthDate(List.of(birthDate))
                                .withAddress(List.of(address1, address2))
                                .build())
                .build();
    }
}
