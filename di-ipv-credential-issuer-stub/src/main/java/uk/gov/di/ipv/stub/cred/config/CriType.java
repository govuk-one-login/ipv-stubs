package uk.gov.di.ipv.stub.cred.config;

public enum CriType {
    EVIDENCE_CRI_TYPE("EVIDENCE"),
    EVIDENCE_DRIVING_LICENCE_CRI_TYPE("EVIDENCE_DRIVING_LICENCE"),
    ACTIVITY_CRI_TYPE("ACTIVITY"),
    FRAUD_CRI_TYPE("FRAUD"),
    VERIFICATION_CRI_TYPE("VERIFICATION"),
    USER_ASSERTED_CRI_TYPE("USER_ASSERTED"),
    DOC_CHECK_APP_CRI_TYPE("DOC_CHECK_APP"),
    F2F_CRI_TYPE("F2F"),
    NINO_CRI_TYPE("NINO");

    public final String value;

    private CriType(String value) {
        this.value = value;
    }

    public static CriType fromValue(String value) {
        for (CriType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
