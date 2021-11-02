package uk.gov.di.ipv.stub.orc.handlers;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import spark.Request;
import spark.Response;
import spark.Route;

import java.net.URI;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_REDIRECT_URL;

public class IpvHandler {

    public Route doAuthorize = (Request request, Response response) -> {
        var state = new State();

        var authRequest = new AuthorizationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE), new ClientID(IPV_CLIENT_ID))
                .state(state)
                .scope(new Scope("openid"))
                .redirectionURI(new URI(ORCHESTRATOR_REDIRECT_URL))
                .endpointURI(new URI(IPV_ENDPOINT).resolve("/oauth2/authorize"))
                .build();

        response.redirect(authRequest.toURI().toString());
        return null;
    };
}
