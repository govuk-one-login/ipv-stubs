package uk.gov.di.ipv.stub.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.util.Base64URL;

public class WellKnownJwk {
    private String kty;
    private Base64URL n;
    private Base64URL e;
    private String use;
    private String kid;
    private String alg;

    public WellKnownJwk(
            @JsonProperty(value = "e", required = true) String e,
            @JsonProperty(value = "kty", required = true) String kty,
            @JsonProperty(value = "n", required = true) String n,
            @JsonProperty(value = "kid", required = true) String kid,
            @JsonProperty(value = "use", required = true) String use,
            @JsonProperty(value = "alg", required = true) String alg) {
        //        this.e = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        this.e = new Base64URL(e);
        this.kty = kty;

        //        this.n = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        this.n = new Base64URL(n);
        this.kid = kid;
        this.use = use;
        this.alg = alg;
    }

    public String getKty() {
        return kty;
    }

    public Base64URL getN() {
        return n;
    }

    public Base64URL getE() {
        return e;
    }

    public String getUse() {
        return use;
    }

    public String getKid() {
        return kid;
    }

    public String getAlg() {
        return alg;
    }

    @Override
    public String toString() {
        return "WellKnownJwk{"
                + "kty='"
                + kty
                + '\''
                + ", n='"
                + n
                + '\''
                + ", e='"
                + e
                + '\''
                + ", use='"
                + use
                + '\''
                + ", kid='"
                + kid
                + '\''
                + ", alg='"
                + alg
                + '\''
                + '}';
    }
}
