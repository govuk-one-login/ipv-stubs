package uk.gov.di.ipv.stub.fraud.gateway.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResidentFrom {

    @JsonProperty("fullDateFrom")
    private String fullDateFrom;

    @JsonProperty("yearFrom")
    private String yearFrom;

    @JsonProperty("monthFrom")
    private String monthFrom;

    @JsonProperty("dayFrom")
    private String dayFrom;

    public String getFullDateFrom() {
        return fullDateFrom;
    }

    public void setFullDateFrom(String fullDateFrom) {
        this.fullDateFrom = fullDateFrom;
    }

    public String getYearFrom() {
        return yearFrom;
    }

    public void setYearFrom(String yearFrom) {
        this.yearFrom = yearFrom;
    }

    public String getMonthFrom() {
        return monthFrom;
    }

    public void setMonthFrom(String monthFrom) {
        this.monthFrom = monthFrom;
    }

    public String getDayFrom() {
        return dayFrom;
    }

    public void setDayFrom(String dayFrom) {
        this.dayFrom = dayFrom;
    }
}
