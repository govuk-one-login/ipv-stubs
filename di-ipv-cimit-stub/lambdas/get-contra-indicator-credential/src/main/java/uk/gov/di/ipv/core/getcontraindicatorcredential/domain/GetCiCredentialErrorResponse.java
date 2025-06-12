package uk.gov.di.ipv.core.getcontraindicatorcredential.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetCiCredentialErrorResponse {
    private String result = "fail";
    private String reason;
    private String errorMessage;

    public GetCiCredentialErrorResponse(String reason, String errorMessage) {
        this.errorMessage = errorMessage;
        this.reason = reason;
    }
}
