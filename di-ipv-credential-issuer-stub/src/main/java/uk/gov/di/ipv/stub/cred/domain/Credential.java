package uk.gov.di.ipv.stub.cred.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.util.Map;

@Data
public class Credential {
    @JsonValue
    private final Map<String, Object> jsonAttributes;
}
