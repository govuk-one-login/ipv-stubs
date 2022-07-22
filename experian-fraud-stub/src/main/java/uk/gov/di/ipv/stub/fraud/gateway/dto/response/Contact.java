package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.Address;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.Person;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Contact {

    @JsonProperty("id")
    private String id;

    @JsonProperty("person")
    private Person person;

    @JsonProperty("addresses")
    private List<Address> addresses = new ArrayList<>();

    @JsonProperty("personDetails")
    private PersonDetails personDetails;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public PersonDetails getPersonDetails() {
        return personDetails;
    }

    public void setPersonDetails(PersonDetails personDetails) {
        this.personDetails = personDetails;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }
}
