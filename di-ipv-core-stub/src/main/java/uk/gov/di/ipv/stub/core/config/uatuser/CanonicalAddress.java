package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record CanonicalAddress(
        String buildingNumber,
        String buildingName,
        String streetName,
        String addressLocality,
        String postalCode,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate validFrom,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                LocalDate validUntil) {}
