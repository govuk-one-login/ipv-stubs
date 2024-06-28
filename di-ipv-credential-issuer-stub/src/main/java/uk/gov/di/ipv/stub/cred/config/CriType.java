package uk.gov.di.ipv.stub.cred.config;

public enum CriType {
    ADDRESS_CRI_TYPE("ADDRESS", false),
    EVIDENCE_CRI_TYPE("EVIDENCE", true),
    EVIDENCE_DRIVING_LICENCE_CRI_TYPE("EVIDENCE_DRIVING_LICENCE", true),
    ACTIVITY_CRI_TYPE("ACTIVITY", true),
    FRAUD_CRI_TYPE("FRAUD", true),
    VERIFICATION_CRI_TYPE("VERIFICATION", true),
    USER_ASSERTED_CRI_TYPE("USER_ASSERTED", false),
    DOC_CHECK_APP_CRI_TYPE("DOC_CHECK_APP", true),
    F2F_CRI_TYPE("F2F", true),
    NINO_CRI_TYPE("NINO", true);

    public final String value;
    private final boolean isIdentityCheck;

    private CriType(String value, boolean isIdentityCheck) {
        this.value = value;
        this.isIdentityCheck = isIdentityCheck;
    }

    public boolean isIdentityCheck() {
        return isIdentityCheck;
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
