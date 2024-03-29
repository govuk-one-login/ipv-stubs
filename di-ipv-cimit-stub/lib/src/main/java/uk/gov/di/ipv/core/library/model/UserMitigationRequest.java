package uk.gov.di.ipv.core.library.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserMitigationRequest {

    @JsonProperty("mitigations")
    private List<String> mitigations;

    private String vcJti;
}
