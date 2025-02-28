package uk.gov.di.ipv.core.getcontraindicatorcredential.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetCiCredentialErrorResponse {
    private String errorType;
    private String errorMessage;
}
