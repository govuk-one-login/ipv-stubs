package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.DrivingPermitDetails;
import uk.gov.di.model.IdCardDetails;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;
import uk.gov.di.model.PassportDetails;
import uk.gov.di.model.ResidencePermitDetails;

import java.util.List;

public class CriStubDataF2f {

    private CriStubDataF2f() {
        // Replace default public constructor
    }

    public static List<IdentityCheckSubjectCriStubData> getData() {
        return List.of(
                createData(
                        "Alice Parker (Valid) DVLA Licence",
                        Names.Alice_Jane_Parker,
                        BirthDates.ALICE_PARKER,
                        DrivingLicences.getAliceParkerDvla()),
                createData(
                        "Claire Aarts DVLA Licence (DWP)",
                        Names.Claire_Aarts,
                        BirthDates.CLAIRE_AARTS,
                        DrivingLicences.getClaireAartsDvla()),
                createData(
                        "Kabir Singh DVLA Licence (DWP)",
                        Names.Kabir_Singh,
                        BirthDates.KABIR_SINGH,
                        DrivingLicences.getKabirSinghDvla()),
                createData(
                        "Kenneth Decerqueira (Valid Passport)",
                        Names.Kenneth_Decerqueira,
                        BirthDates.KENNETH_DECERQUEIRA,
                        Passports.KENNETH_DECERQUEIRA_PASSPORT),
                createData(
                        "Nora Porter DVLA Licence (DWP)",
                        Names.Nora_Porter,
                        BirthDates.NORA_PORTER,
                        DrivingLicences.getNoraPorterDvla()),
                createData(
                        "Mary Watson (Valid Passport)",
                        Names.Mary_Watson,
                        BirthDates.MARY_WATSON,
                        Passports.MARY_WATSON_F2F_PASSPORT),
                createData(
                        "Saul Goodman BRC",
                        Names.Saul_Goodman,
                        BirthDates.SAUL_GOODMAN,
                        ResidencePermits.SAUL_GOODMAN_BRC),
                createData(
                        "Saul Goodman (Valid EEA Card)",
                        Names.Saul_Goodman,
                        BirthDates.SAUL_GOODMAN,
                        IdCards.EEA_VALID),
                createData(
                        "Tom Hardy DVLA Licence (DWP)",
                        Names.Tom_Hardy,
                        BirthDates.TOM_HARDY,
                        DrivingLicences.getTomHardyDvla()));
    }

    private static IdentityCheckSubjectCriStubData createData(
            String label, Name name, BirthDate birthDate, DrivingPermitDetails drivingLicences) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(
                        IdentityCheckSubject.builder()
                                .withName(List.of(name))
                                .withBirthDate(List.of(birthDate))
                                .withDrivingPermit(List.of(drivingLicences))
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

    private static IdentityCheckSubjectCriStubData createData(
            String label, Name name, BirthDate birthDate, IdCardDetails idCard) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(
                        IdentityCheckSubject.builder()
                                .withName(List.of(name))
                                .withBirthDate(List.of(birthDate))
                                .withIdCard(List.of(idCard))
                                .build())
                .build();
    }
}
