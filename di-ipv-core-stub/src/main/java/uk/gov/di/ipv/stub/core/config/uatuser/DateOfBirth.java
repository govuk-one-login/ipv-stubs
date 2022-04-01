package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public record DateOfBirth(@JsonProperty("value") Date dob) {}
