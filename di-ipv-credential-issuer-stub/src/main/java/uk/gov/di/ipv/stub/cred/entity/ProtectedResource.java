package uk.gov.di.ipv.stub.cred.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.util.Map;

@Data
public class ProtectedResource {
    @JsonValue
    private final Map<String, Object> jsonAttributes;
}
