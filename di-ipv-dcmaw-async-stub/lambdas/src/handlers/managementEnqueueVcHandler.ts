import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { importPKCS8, SignJWT } from "jose";
import { buildApiResponse } from "../common/apiResponse";
import getErrorMessage from "../common/errorReporting";
import { buildMockVc } from "../domain/mockVc";
import { ManagementEnqueueVcRequest } from "../domain/managementEnqueueRequest";
import { getState } from "../services/userStateService";
import getConfig from "../common/config";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    const config = await getConfig();

    if (event.body === undefined) {
      return buildApiResponse({ errorMessage: "No request body" }, 400);
    }

    const requestBody = JSON.parse(event.body);

    if (!requestBody.test_user) {
      // We do not want to produce a VC, only to return the oauth state to allows API tests to callback as the mobile
      // app, which includes it in the callback endpoint as a query parameter.
      const state = await getState(requestBody.user_id);
      return buildApiResponse(
        {
          result: "success",
          oauthState: state,
        },
        201,
      );
    }

    const parsedBody = parseRequest(event.body);

    if (typeof parsedBody === "string") {
      return buildApiResponse({ errorMessage: parsedBody }, 400);
    }

    const vc = await buildMockVc(
      parsedBody.user_id,
      parsedBody.test_user,
      parsedBody.document_type,
      parsedBody.evidence_type,
      parsedBody.ci,
    );

    const signingKey = await importPKCS8(
      `-----BEGIN PRIVATE KEY-----\n${config.vcSigningKey}\n-----END PRIVATE KEY-----`, // pragma: allowlist secret - the key is coming from config
      "ES256",
    );
    const signedJwt = await new SignJWT(vc)
      .setProtectedHeader({ alg: "ES256", typ: "JWT" })
      .sign(signingKey);

    const state = await getState(parsedBody.user_id);

    const queueMessage = {
      sub: parsedBody.user_id,
      state,
      "https://vocab.account.gov.uk/v1/credentialJWT": [signedJwt],
    };

    await fetch(config.queueStubUrl, {
      method: "POST",
      headers: { "x-api-key": config.queueStubApiKey },
      body: JSON.stringify({
        queueName: parsedBody.queue_name ?? config.queueName,
        queueEvent: queueMessage,
        delaySeconds: parsedBody.delay_seconds ?? 0,
      }),
    });

    return buildApiResponse(
      {
        result: "success",
        // Returning state allows API tests to callback as the mobile app.
        oauthState: state,
      },
      201,
    );
  } catch (error) {
    return buildApiResponse(
      { errorMessage: "Unexpected error: " + getErrorMessage(error) },
      500,
    );
  }
}

function parseRequest(body: string): string | ManagementEnqueueVcRequest {
  const requestBody = JSON.parse(body);

  const mandatoryFields = [
    "user_id",
    "test_user",
    "document_type",
    "evidence_type",
  ];
  const missingFields = mandatoryFields.filter(
    (field) => requestBody[field] === undefined,
  );

  if (missingFields.length > 0) {
    return "Request body is missing fields: " + missingFields.join(", ");
  }

  return requestBody as ManagementEnqueueVcRequest;
}
