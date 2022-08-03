package uk.gov.di.ipv.stub.fraud;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Util {

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);
    private static ObjectMapper mapper = new ObjectMapper();

    private Util() {}

    public static InputStream getResourceAsStream(String s) {
        return Handler.class.getResourceAsStream(s);
    }

    public static <T> T mapFileToObject(InputStream inputStream, Class<T> clazz) {
        try {
            String fileContentAsString = readFromInputStream(inputStream);
            return (T) mapper.readValue(fileContentAsString, clazz);
        } catch (Exception e) {
            LOGGER.error("Failed to map file to object ", e);
            return null;
        }
    }

    private static String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
