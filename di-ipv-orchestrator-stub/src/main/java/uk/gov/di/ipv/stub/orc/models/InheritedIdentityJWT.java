package uk.gov.di.ipv.stub.orc.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Arrays;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InheritedIdentityJWT {
    private final String[] values;

    public InheritedIdentityJWT(String... jwtValues) {
        this.values = jwtValues;
    }

    public String[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "InheritedIdentityJWT{" + "values=" + Arrays.toString(values) + '}';
    }
}
