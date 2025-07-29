package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;
import uk.gov.di.model.DrivingPermitDetails;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;

import java.util.List;

public class CriStubDataDrivingLicence {

    private CriStubDataDrivingLicence() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data =
            List.of(
                    createData(
                            "Alice Parker (Valid) DVLA Licence",
                            Names.Alice_Jane_Parker,
                            BirthDates.ALICE_PARKER,
                            DrivingLicences.ALICE_PARKER_DVLA),
                    createData(
                            "Alice Parker (Changed First Name) DVLA Licence",
                            Names.Alison_Jane_Parker,
                            BirthDates.ALICE_PARKER,
                            DrivingLicences.ALICE_PARKER_DVLA),
                    createData(
                            "Alice Parker (Changed Last Name) DVLA Licence",
                            Names.Alice_Jane_Smith,
                            BirthDates.ALICE_PARKER,
                            DrivingLicences.ALICE_PARKER_DVLA),
                    createData(
                            "Bob Parker (Valid) DVA Licence",
                            Names.Bob_Parker,
                            BirthDates.BOB_PARKER,
                            DrivingLicences.BOB_PARKER_DVA),
                    createData(
                            "Claire Aarts DVLA Licence (DWP)",
                            Names.Claire_Aarts,
                            BirthDates.CLAIRE_AARTS,
                            DrivingLicences.CLAIRE_AARTS_DVLA),
                    createData(
                            "Kabir Singh DVLA Licence (DWP)",
                            Names.Kabir_Singh,
                            BirthDates.KABIR_SINGH,
                            DrivingLicences.KABIR_SINGH_DVLA),
                    createData(
                            "Kenneth Decerqueira (Valid) DVLA Licence",
                            Names.Kenneth_Decerqueira,
                            BirthDates.KENNETH_DECERQUEIRA,
                            DrivingLicences.KENNETH_DECERQUEIRA_DVLA),
                    createData(
                            "Nora Porter DVLA Licence (DWP)",
                            Names.Nora_Porter,
                            BirthDates.NORA_PORTER,
                            DrivingLicences.NORA_PORTER_DVLA),
                    createData(
                            "Tom Hardy DVLA Licence (DWP)",
                            Names.Tom_Hardy,
                            BirthDates.TOM_HARDY,
                            DrivingLicences.TOM_HARDY_DVLA));

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
}
