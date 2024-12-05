package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CanonicalAddress(
        String buildingNumber,
        String buildingName,
        String streetName,
        String addressLocality,
        String postalCode,
        String addressCountry,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate validFrom,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                LocalDate validUntil) {}
