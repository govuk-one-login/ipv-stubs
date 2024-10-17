package uk.gov.di.ipv.stub.core.config.uatuser;

import spark.utils.StringUtils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public record FullName(String firstName, String middleName, String surname) {
    public String fullName() {
        return Stream.of(
                        nonBlankValueOrBlank(firstName),
                        nonBlankValueOrBlank(middleName),
                        nonBlankValueOrBlank(surname))
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String nonBlankValueOrBlank(String value) {
        if (StringUtils.isNotBlank(value)) {
            return value.trim();
        } else {
            return "";
        }
    }
}
