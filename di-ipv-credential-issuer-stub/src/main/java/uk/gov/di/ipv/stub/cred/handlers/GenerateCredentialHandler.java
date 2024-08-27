package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import uk.gov.di.ipv.stub.cred.domain.Credential;
import uk.gov.di.ipv.stub.cred.domain.GenerateCredentialRequest;
import uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialGenerator;

import java.util.Map;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

// This endpoint generates stub VCs without going through an OAuth flow
// It is only intended to be used as part of test setup
public class GenerateCredentialHandler {
    public static final String JWT_CONTENT_TYPE = "application/jwt;charset=UTF-8";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(INDENT_OUTPUT);
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE =
            new TypeReference<>() {};

    private final VerifiableCredentialGenerator verifiableCredentialGenerator;

    public GenerateCredentialHandler(VerifiableCredentialGenerator verifiableCredentialGenerator) {
        this.verifiableCredentialGenerator = verifiableCredentialGenerator;
    }

    public void generateCredential(Context ctx) throws Exception {
        var request = ctx.bodyAsClass(GenerateCredentialRequest.class);
        var vc =
                verifiableCredentialGenerator.generate(
                        new Credential(
                                OBJECT_MAPPER.readValue(
                                        request.credentialSubjectJson(), MAP_TYPE_REFERENCE),
                                OBJECT_MAPPER.readValue(request.evidenceJson(), MAP_TYPE_REFERENCE),
                                request.userId(),
                                request.clientId(),
                                request.nbf()));
        ctx.contentType(JWT_CONTENT_TYPE);
        ctx.status(HttpStatus.CREATED);
        ctx.result(vc.toString());
    }
}
