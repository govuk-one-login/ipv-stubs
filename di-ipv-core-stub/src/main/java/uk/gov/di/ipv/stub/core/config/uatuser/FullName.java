package uk.gov.di.ipv.stub.core.config.uatuser;

import spark.utils.StringUtils;

public record FullName(String firstName, String middleName, String surname) {
    public String fullName() {
        return "%s %s %s"
                .formatted(
                        nonBlankValueOrBlank(firstName),
                        nonBlankValueOrBlank(middleName),
                        nonBlankValueOrBlank(surname))
                .trim();
    }

    private String nonBlankValueOrBlank(String value) {
        if (StringUtils.isNotBlank(value)) {
            return value.trim();
        } else {
            return "";
        }
    }
}
