package uk.gov.di.ipv.stub.core.config.uatuser;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.di.ipv.stub.core.utils.StringHelper.isNotBlank;

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
        if (isNotBlank(value)) {
            return value.trim();
        } else {
            return "";
        }
    }
}
