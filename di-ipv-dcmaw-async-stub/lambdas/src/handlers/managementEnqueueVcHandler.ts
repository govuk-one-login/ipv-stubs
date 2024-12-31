import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { importPKCS8, SignJWT } from "jose";
import { buildApiResponse } from "../common/apiResponse";
import getErrorMessage from "../common/errorReporting";
import { buildMockVc } from "../domain/mockVc";
import { ManagementEnqueueVcRequest } from "../domain/managementEnqueueRequest";
import { popState } from "../services/userStateService";
import getConfig from "../common/config";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    const config = await getConfig();

    if (event.body === undefined) {
      return buildApiResponse({ errorMessage: "No request body" }, 400);
    }

    const requestBody = parseRequest(event);
    if (typeof requestBody === "string") {
      return buildApiResponse({ errorMessage: requestBody }, 400);
    }

    const vc = await buildMockVc(
      requestBody.user_id,
      requestBody.test_user,
      requestBody.document_type,
      requestBody.evidence_type,
      requestBody.ci,
    );

    const signingKey = await importPKCS8(
      `-----BEGIN PRIVATE KEY-----\n${config.vcSigningKey}\n-----END PRIVATE KEY-----`, // pragma: allowlist secret - the key is coming from config
      "ES256",
    );
    const signedJwt = await new SignJWT(vc)
      .setProtectedHeader({ alg: "ES256", typ: "JWT" })
      .sign(signingKey);

    const state = await popState(requestBody.user_id);

    const queueMessage = {
      sub: requestBody.user_id,
      state,
      "https://vocab.account.gov.uk/v1/credentialJWT": [signedJwt],
    };

    await fetch(config.queueStubUrl, {
      method: "POST",
      headers: { "x-api-key": config.queueStubApiKey },
      body: JSON.stringify({
        queueName: requestBody.queue_name ?? config.queue_name,
        queueEvent: queueMessage,
        delaySeconds: requestBody.delay_seconds ?? 0,
      }),
    });

    return buildApiResponse(
      {
        result: "success",
        oauthState: state
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

function parseRequest(
  event: APIGatewayProxyEventV2,
): string | ManagementEnqueueVcRequest {
  if (event.body === undefined) {
    return "No request body";
  }

  const requestBody = JSON.parse(event.body);

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
