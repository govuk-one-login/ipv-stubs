package uk.gov.di.ipv.stub.cred.config;

public enum CriType {
    EVIDENCE_CRI_TYPE("evidence"),
    ACTIVITY_CRI_TYPE("activity"),
    FRAUD_CRI_TYPE("fraud"),
    VERIFICATION_CRI_TYPE("verification");

    public final String value;

    private CriType(String value) {
        this.value = value;
    }
}
