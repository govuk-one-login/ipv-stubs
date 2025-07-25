package uk.gov.di.ipv.stub.cred.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import uk.gov.di.model.IdentityCheckSubject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record IdentityCheckSubjectCriStubData(String label, IdentityCheckSubject payload) {}
