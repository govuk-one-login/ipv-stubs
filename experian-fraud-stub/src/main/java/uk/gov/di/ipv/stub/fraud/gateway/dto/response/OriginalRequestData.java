package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.Application;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.Contact;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(
        ignoreUnknown = true,
        value = {"otherData"})
public class OriginalRequestData implements Serializable {
    @JsonProperty("application")
    private Application application = new Application();

    @JsonProperty("contacts")
    private List<Contact> contacts = new ArrayList<>();

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }
}
