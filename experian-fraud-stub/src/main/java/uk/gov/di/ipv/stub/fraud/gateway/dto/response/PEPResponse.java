package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(value = "originalRequestData")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PEPResponse extends IdentityVerificationResponse {}
