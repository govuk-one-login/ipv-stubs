package uk.gov.di.ipv.stub.cred.domain;

public record ApiDecryptJarRequest (
        String clientId,
        String request) implements DecryptJarRequest {}
