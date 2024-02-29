package uk.gov.di.ipv.core.putcontraindicators.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Evidence(String type, List<String> ci, String txn) {}
