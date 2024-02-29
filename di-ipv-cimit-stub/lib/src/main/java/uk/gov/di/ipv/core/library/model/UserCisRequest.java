package uk.gov.di.ipv.core.library.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserCisRequest {
    private String code;
    private String issuanceDate;
    private String issuer;
    private List<String> mitigations;
    private String document;
}
