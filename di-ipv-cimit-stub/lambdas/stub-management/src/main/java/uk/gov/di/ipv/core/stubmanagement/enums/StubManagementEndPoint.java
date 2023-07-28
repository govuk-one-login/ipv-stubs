package uk.gov.di.ipv.core.stubmanagement.enums;

public enum StubManagementEndPoint {
    USER_CIS("/user/{userId}/cis"),
    USER_MITIGATIONS("/user/{userId}/mitigations/{ci}");

    private final String path;

    StubManagementEndPoint(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
