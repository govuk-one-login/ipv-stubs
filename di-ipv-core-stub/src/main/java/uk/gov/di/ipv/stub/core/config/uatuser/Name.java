package uk.gov.di.ipv.stub.core.config.uatuser;

import spark.utils.StringUtils;

public record Name(String firstName, String surname) {

    public String firstLastName() {
        return "%s %s".formatted(nonBlankValueOrBlank(firstName), nonBlankValueOrBlank(surname)).trim();
    }

    private String nonBlankValueOrBlank(String value) {
        if (StringUtils.isNotBlank(value)) {
            return value.trim();
        } else {
            return "";
        }
    }
}
