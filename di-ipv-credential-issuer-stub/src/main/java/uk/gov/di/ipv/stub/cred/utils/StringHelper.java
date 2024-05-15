package uk.gov.di.ipv.stub.cred.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class StringHelper {
    private StringHelper() {}

    public static List<String> splitCommaDelimitedStringValue(String toSplit) {
        return Stream.ofNullable(toSplit)
                .flatMap(cisString -> Arrays.stream(cisString.split(",", -1)))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
