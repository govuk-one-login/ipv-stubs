package uk.gov.di.ipv.stub.cred.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class F2FEnqueueLambdaRequest {
    private String queueName;
    private F2FQueueEvent queueEvent;
    private int delaySeconds;
}
