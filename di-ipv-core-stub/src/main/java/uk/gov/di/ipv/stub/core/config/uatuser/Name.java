package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Name(@JsonProperty("nameParts") List<NameParts> nameParts) {}
