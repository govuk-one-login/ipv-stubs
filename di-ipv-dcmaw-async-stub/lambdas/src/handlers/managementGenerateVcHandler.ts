import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import {buildApiResponse, buildApiResponseFromString} from "../common/apiResponse";
import getErrorMessage from "../common/errorReporting";
import { buildMockVcFromSubjectAndEvidence } from "../domain/mockVc";
import {
  isManagementEnqueueVcRequestEvidenceAndSubject,
} from "../domain/managementEnqueueRequest";
import getConfig from "../common/config";
import {vcToSignedJwt} from "../domain/signedJwt";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    const config = await getConfig();

    if (event.body === undefined) {
      return buildApiResponse({ errorMessage: "No request body" }, 400);
    }

    const requestBody = JSON.parse(event.body);

    if (!isManagementEnqueueVcRequestEvidenceAndSubject(requestBody)) {
      return buildApiResponse({ errorMessage: "Unrecognised request body" }, 400);
    }

    const vc = await buildMockVcFromSubjectAndEvidence(
        requestBody.user_id,
        requestBody.credential_subject,
        requestBody.evidence,
        requestBody.nbf,
      );

    const signedJwt = await vcToSignedJwt(vc, config.vcSigningKey);

    return buildApiResponseFromString(
        signedJwt,
        200,
    );
  } catch (error) {
    return buildApiResponse(
      { errorMessage: "Unexpected error: " + getErrorMessage(error) },
      500,
    );
  }
}
