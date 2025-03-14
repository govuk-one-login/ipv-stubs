package uk.gov.di.ipv.stub.cred.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CriResponseQueueErrorEvent {
    private String sub;
    private String state;
    private String error;
    private String error_description;
}
