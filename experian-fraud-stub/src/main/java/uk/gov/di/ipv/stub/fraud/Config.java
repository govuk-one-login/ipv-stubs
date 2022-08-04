package uk.gov.di.ipv.stub.fraud;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Config {
    public static final String PORT = getConfigValue("PORT", "8080");
    public static final List<String> ciList;
    public static final HashMap<String, String[]> ciMap;
    public static final List<String> pepList;
    public static final HashMap<String, String[]> pepMap;

    static {
        ciList = List.of("CI1", "CI2", "CI3", "CI4", "CI5");
        ciMap = new HashMap<>();
        for (String ci : ciList) {
            ciMap.put(ci, getConfigValue(ci));
        }

        pepList =
                List.of(
                        "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10", "P11", "P12",
                        "P13", "P14", "P15", "P16", "P17", "P18", "P19", "P20", "P21", "P22", "P23",
                        "P24", "P25", "P26", "P27", "P28", "P29", "P30", "P31", "P32", "P33", "P34",
                        "P35", "P36");
        pepMap = new HashMap<>();
        for (String pep : pepList) {
            pepMap.put(pep, getConfigValue(pep));
        }
    }

    private static String getConfigValue(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(key))
                .orElse(Objects.requireNonNull(defaultValue, "no default value"));
    }

    private static String[] getConfigValue(String key) {
        return Optional.ofNullable(System.getenv(key)).orElse("").split(",");
    }
}
