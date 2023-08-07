package uk.gov.di.ipv.stub.cred.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class F2FErrorEnqueueLambdaRequest {
    private String queueName;
    private F2FQueueErrorEvent queueEvent;
    private int delaySeconds;
}
