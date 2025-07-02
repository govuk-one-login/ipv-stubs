import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponse";
import getErrorMessage from "../common/errorReporting";
import {
  buildMockVc,
  buildMockVcFromSubjectAndEvidence,
} from "../domain/mockVc";
import {
  isManagementEnqueueVcRequest,
  isManagementEnqueueVcRequestEvidenceAndSubject,
  isManagementEnqueueVcRequestIndividualDetails,
} from "../domain/managementEnqueueRequest";
import { getUserStateItem } from "../services/userStateService";
import getConfig from "../common/config";
import { JWTPayload } from "jose/dist/types/types";
import { vcToSignedJwt } from "../domain/signedJwt";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    const config = await getConfig();

    if (event.body === undefined) {
      return buildApiResponse({ errorMessage: "No request body" }, 400);
    }

    const requestBody = JSON.parse(event.body);

    if (!isManagementEnqueueVcRequest(requestBody)) {
      return buildApiResponse({ errorMessage: "Missing user_id field" }, 400);
    }

    const queueNameFromRequest = requestBody.queue_name;
    const delaySeconds = requestBody.delay_seconds;
    const { state, journeyId } = await getUserStateItem(requestBody.user_id);
    let vc: JWTPayload;

    if (isManagementEnqueueVcRequestIndividualDetails(requestBody)) {
      vc = await buildMockVc(
        requestBody.user_id,
        requestBody.test_user,
        requestBody.document_type,
        requestBody.evidence_type,
        requestBody.ci,
      );
    } else if (isManagementEnqueueVcRequestEvidenceAndSubject(requestBody)) {
      vc = await buildMockVcFromSubjectAndEvidence(
        requestBody.user_id,
        requestBody.credential_subject,
        requestBody.evidence,
        requestBody.nbf,
      );

      if (requestBody.mitigated_cis) {
        await mitigateCis(
          requestBody.mitigated_cis.mitigatedCis,
          requestBody.mitigated_cis.cimitStubUrl,
          requestBody.mitigated_cis.cimitStubApiKey,
          requestBody.user_id,
          vc.jti,
        );
      }
    } else {
      // We do not want to produce a VC, only to return the oauth state to allows API tests to callback as the mobile
      // app, which includes the state in the callback endpoint as a query parameter.
      return buildApiResponse(
        {
          result: "success",
          oauthState: state,
        },
        201,
      );
    }

    const signedJwt = await vcToSignedJwt(vc, config.vcSigningKey);

    const queueMessage = {
      sub: vc.sub,
      state,
      govuk_signin_journey_id: journeyId,
      "https://vocab.account.gov.uk/v1/credentialJWT": [signedJwt],
    };

    const queueName = queueNameFromRequest ?? config.queueName;

    const postResult = await fetch(config.queueStubUrl, {
      method: "POST",
      headers: { "x-api-key": config.queueStubApiKey },
      body: JSON.stringify({
        queueName: queueName,
        queueEvent: queueMessage,
        delaySeconds: delaySeconds ?? 0,
      }),
    });

    if (!postResult.ok) {
      throw new Error(`Failed to enqueue VC: ${await postResult.text()}`);
    }

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

async function mitigateCis(
  mitigatedCis: string[],
  cimitStubUrl: string,
  cimitStubApiKey: string,
  userId: string,
  jwtId: string | undefined,
) {
  if (mitigatedCis.length === 0) {
    return;
  }
  if (!jwtId) {
    throw new Error("Missing JWT ID");
  }

  for (const ciToMitigate of mitigatedCis) {
    const cimitUrl = `${cimitStubUrl}/user/${encodeURIComponent(userId)}/mitigations/${encodeURIComponent(ciToMitigate)}`;

    const postResult = await fetch(cimitUrl, {
      method: "POST",
      headers: {
        "x-api-key": cimitStubApiKey,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        mitigations: ["M01"],
        vcJti: jwtId,
      }),
    });

    if (!postResult.ok) {
      throw new Error(
        `Failed to mitigate CI ${ciToMitigate}: ${await postResult.text()}`,
      );
    }
  }
}
