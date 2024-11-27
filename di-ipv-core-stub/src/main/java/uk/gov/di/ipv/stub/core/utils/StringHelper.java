package uk.gov.di.ipv.stub.core.utils;

public class StringHelper {

    private StringHelper() {}

    public static Boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public static Boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean isEmpty(String str) {
        return (str == null || str.isEmpty());
    }
}
