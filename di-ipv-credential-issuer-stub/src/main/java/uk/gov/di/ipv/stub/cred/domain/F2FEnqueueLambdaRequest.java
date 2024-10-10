package uk.gov.di.ipv.stub.cred.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class F2FEnqueueLambdaRequest {
    private String queueName;
    private CriResponseQueueEvent queueEvent;
    private int delaySeconds;
}
