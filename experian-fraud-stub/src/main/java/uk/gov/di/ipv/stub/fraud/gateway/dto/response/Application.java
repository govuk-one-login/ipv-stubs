package uk.gov.di.ipv.stub.fraud.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.Applicant;
import uk.gov.di.ipv.stub.fraud.gateway.dto.request.ProductDetails;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Application {
    @JsonProperty("applicants")
    private List<Applicant> applicants = new ArrayList<>();

    @JsonProperty("productDetails")
    private ProductDetails productDetails;

    @JsonProperty("type")
    private String type;

    public List<Applicant> getApplicants() {
        return applicants;
    }

    public void setApplicants(List<Applicant> applicants) {
        this.applicants = applicants;
    }

    public ProductDetails getProductDetails() {
        return productDetails;
    }

    public void setProductDetails(ProductDetails productDetails) {
        this.productDetails = productDetails;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
