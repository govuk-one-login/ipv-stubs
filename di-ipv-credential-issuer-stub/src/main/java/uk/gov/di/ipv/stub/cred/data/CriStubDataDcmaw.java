package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.DrivingPermitDetails;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;
import uk.gov.di.model.PassportDetails;
import uk.gov.di.model.PostalAddress;
import uk.gov.di.model.ResidencePermitDetails;

import java.util.List;

public class CriStubDataDcmaw {

    private CriStubDataDcmaw() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data =
            List.of(
                    createData(
                            "Alice Doe (Valid) NFC Passport",
                            Names.ALICE_JANE_LAURA_DOE,
                            BirthDates.ALICE_DOE,
                            Passports.ALICE_DOE_PASSPORT),
                    createData(
                            "Alice Parker (Valid) BRC",
                            Names.ALICE_JANE_PARKER,
                            BirthDates.ALICE_PARKER,
                            ResidencePermits.ALICE_PARKER_BRC),
                    createData(
                            "Alice Parker BRP",
                            Names.ALICE_JANE_PARKER,
                            BirthDates.ALICE_PARKER,
                            ResidencePermits.ALICE_PARKER_BRP),
                    createData(
                            "Alice Parker (Valid) DVLA Licence",
                            Names.ALICE_JANE_PARKER,
                            BirthDates.ALICE_PARKER,
                            DrivingLicences.ALICE_PARKER_DVLA),
                    createData(
                            "Alice Parker (Changed First Name) DVLA Licence",
                            Names.ALISON_JANE_PARKER,
                            BirthDates.ALICE_PARKER,
                            DrivingLicences.ALICE_PARKER_DVLA),
                    createData(
                            "Alice Parker (Changed Last Name) DVLA Licence",
                            Names.ALICE_JANE_SMITH,
                            BirthDates.ALICE_PARKER,
                            DrivingLicences.ALICE_PARKER_DVLA),
                    createData(
                            "Billy Batson (Valid) DVA Licence",
                            Names.Billy_Batson,
                            BirthDates.BILLY_BATSON,
                            DrivingLicences.BILLY_BATSON_DVA),
                    createData(
                            "Claire Aarts DVLA Licence (DWP)",
                            Names.CLAIRE_AARTS,
                            BirthDates.CLAIRE_AARTS,
                            DrivingLicences.CLAIRE_AARTS_DVLA),
                    createData(
                            "Joe Shmoe (Valid) Driving Licence",
                            Names.Joe_Schmoe,
                            BirthDates.JOE_SHMOE,
                            DrivingLicences.JOE_SHMOE_DVLA,
                            Addresses.JOE_SCHMOE_ADDRESS),
                    createData(
                            "John Roberts (Invalid) DVA Licence",
                            Names.John_Roberts,
                            BirthDates.JOHN_ROBERTS,
                            DrivingLicences.JOHN_ROBERTS_DVA_INVALID),
                    createData(
                            "Kabir Singh DVLA Licence (DWP)",
                            Names.KABIR_SINGH,
                            BirthDates.KABIR_SINGH,
                            DrivingLicences.KABIR_SINGH_DVLA),
                    createData(
                            "Kenneth Decerqueira (Valid Experian) Passport",
                            Names.KENNETH_DECERQUEIRA,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Passports.KENNETH_DECERQUEIRA_PASSPORT),
                    createData(
                            "Kenneth Decerqueira (Changed First Name) Passport",
                            Names.MICHAEL_DECERQUEIRA,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Passports.KENNETH_DECERQUEIRA_PASSPORT),
                    createData(
                            "Kenneth Decerqueira (Changed Last Name) Passport",
                            Names.KENNETH_JONES,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Passports.KENNETH_DECERQUEIRA_PASSPORT),
                    createData(
                            "Kenneth Decerqueira (Valid) DVLA Licence",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            DrivingLicences.KENNETH_DECERQUEIRA_DVLA),
                    createData(
                            "Kenneth Decerqueira (Valid) DVLA Licence 2",
                            Names.KENNETH_DECERQUEIRA,
                            BirthDates.KENNETH_DECERQUEIRA,
                            DrivingLicences.KENNETH_DECERQUEIRA_DVLA_2),
                    createData(
                            "Kenneth Decerqueira (Valid) DVA Licence",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            DrivingLicences.KENNETH_DECERQUEIRA_DVA),
                    createData(
                            "Kenneth Decerqueira (Invalid) DVLA Licence",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            DrivingLicences.KENNETH_DECERQUEIRA_DVLA_INVALID),
                    createData(
                            "Kenneth Decerqueira (Invalid) DVLA Licence 2",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            DrivingLicences.KENNETH_DECERQUEIRA_DVLA_2_INVALID),
                    createData(
                            "Kenneth Decerqueira (International)",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            Passports.KENNETH_DECERQUEIRA_INTERNATIONAL_PASSPORT),
                    createData(
                            "Nora Porter DVLA Licence (DWP)",
                            Names.NORA_PORTER,
                            BirthDates.NORA_PORTER,
                            DrivingLicences.NORA_PORTER_DVLA),
                    createData(
                            "Saul Goodman BRC",
                            Names.SAUL_GOODMAN,
                            BirthDates.SAUL_GOODMAN,
                            ResidencePermits.SAUL_GOODMAN_BRC),
                    createData(
                            "Tom Hardy DVLA Licence (DWP)",
                            Names.TOM_HARDY,
                            BirthDates.TOM_HARDY,
                            DrivingLicences.TOM_HARDY_DVLA));

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

    private static IdentityCheckSubjectCriStubData createData(
            String label, Name name, BirthDate birthDate, DrivingPermitDetails drivingLicence) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(
                        IdentityCheckSubject.builder()
                                .withName(List.of(name))
                                .withBirthDate(List.of(birthDate))
                                .withDrivingPermit(List.of(drivingLicence))
                                .build())
                .build();
    }

    private static IdentityCheckSubjectCriStubData createData(
            String label,
            Name name,
            BirthDate birthDate,
            DrivingPermitDetails drivingLicence,
            PostalAddress postalAddress) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(
                        IdentityCheckSubject.builder()
                                .withName(List.of(name))
                                .withBirthDate(List.of(birthDate))
                                .withDrivingPermit(List.of(drivingLicence))
                                .withAddress(List.of(postalAddress))
                                .build())
                .build();
    }

    private static IdentityCheckSubjectCriStubData createData(
            String label, Name name, BirthDate birthDate, ResidencePermitDetails residencePermit) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(
                        IdentityCheckSubject.builder()
                                .withName(List.of(name))
                                .withBirthDate(List.of(birthDate))
                                .withResidencePermit(List.of(residencePermit))
                                .build())
                .build();
    }
}
