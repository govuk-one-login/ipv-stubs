package uk.gov.di.ipv.core.postmitigations.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class PostMitigationsRequestBody {
    @JsonProperty("signed_jwts")
    private List<String> signedJwts;
}
