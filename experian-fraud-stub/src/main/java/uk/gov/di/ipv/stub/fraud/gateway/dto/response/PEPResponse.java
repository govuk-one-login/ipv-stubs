package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonIgnoreProperties(value = "originalRequestData", ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PEPResponse extends IdentityVerificationResponse implements Serializable {}
