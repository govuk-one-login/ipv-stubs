package uk.gov.di.ipv.stub.cred.handlers;

import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;

import java.util.Collections;
import java.util.HashMap;

public class AuthorizeHandler {

    public Route doAuthorize = (Request request, Response response) ->
            ViewHelper.render(Collections.emptyMap(), "authorize.mustache");

    public Route generateAuthCode = (Request request, Response response) -> {
        System.out.println("GENERATE AUTH CODE!!!");
        // TODO: generate auth code here...
        return null;
    };
}
