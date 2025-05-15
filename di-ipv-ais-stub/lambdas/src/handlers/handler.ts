import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../utils/response";
import { getAisResponse } from "../utils/dataLayer";
import cases from "../cases";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse(
      { errorMessage: "Missing user id path param" },
      400,
    );
  }

  try {
    // Get response from DynamoDB
    const response = await getAisResponse(decodeURIComponent(userId));

    // Artificially delay the response
    await delayResponse(response?.responseDelay);

    // Non-200 responses do not have bodies
    if (response?.statusCode === 200) {
      return buildApiResponse(
        response?.responseBody ?? cases["AIS_NO_INTERVENTION"],
        200,
      );
    }

    return buildApiResponse({}, response?.statusCode);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse(
      { errorMessage: error.message },
      error.statusCode || 500,
    );
  }
}

async function delayResponse(responseDelay?: number) {
  if (responseDelay && responseDelay > 0) {
    console.info(
      `response delay configured for ais request - sleeping for ${responseDelay} seconds`,
    );
    await new Promise((r) => setTimeout(r, 1000 * responseDelay));
    console.info("woken up");
  }
}
