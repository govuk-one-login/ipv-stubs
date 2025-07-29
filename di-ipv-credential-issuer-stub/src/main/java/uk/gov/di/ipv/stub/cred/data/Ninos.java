package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.SocialSecurityRecordDetails;

public class Ninos {

    public static final SocialSecurityRecordDetails NINO_VALID = createNino("AA000003D");

    // DWP users
    public static final SocialSecurityRecordDetails NINO_CLAIRE_AARTS = createNino("LB765460D");
    public static final SocialSecurityRecordDetails NINO_KABIR_SINGH = createNino("MW200001A");
    public static final SocialSecurityRecordDetails NINO_NORA_PORTER = createNino("MW000683A");
    public static final SocialSecurityRecordDetails NINO_TOM_HARDY = createNino("MW000749A");

    private Ninos() {
        // Replace default public constructor
    }

    private static SocialSecurityRecordDetails createNino(String personalNumber) {

        return SocialSecurityRecordDetails.builder().withPersonalNumber(personalNumber).build();
    }
}
