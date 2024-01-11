package uk.gov.di.ipv.stub.orc.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InheritedIdentityJWT {
    private String[] value;

    public InheritedIdentityJWT(String... jwtValues) {
        this.value = jwtValues;
    }

    @JsonValue
    public Map<String, String[]> getValue() {
        return Collections.singletonMap("value", value);
    }

    @Override
    public String toString() {
        return "InheritedIdentityJWT{" + "value=" + Arrays.toString(value) + '}';
    }
}
