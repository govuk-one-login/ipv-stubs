import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse, getErrorMessage } from "../get-contra-indicator-credential/apiResponseBuilder";


export const postMitigationsHandler = async (
  request: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> => {
  console.info("Function invoked:", "PostMitigations");
  try {
    const parsedRequest = validateAndParseRequest(request);
    return parsedRequest;

  } catch (error) {
    return buildApiResponse(500, {
          message: getErrorMessage(error),
        });
  }
};

// REMOVE BEFORE RUN
// export interface APIGatewayProxyStructuredResultV2 {
//     statusCode?: number | undefined;
//     headers?:
//         | {
//             [header: string]: boolean | number | string;
//         }
//         | undefined;
//     body?: string | undefined;
//     isBase64Encoded?: boolean | undefined;
//     cookies?: string[] | undefined;
// }

export interface postMitigationsResponse {
  result: string;
  reason?: string;
  errorMessage?: string;
}

export interface postMitigationsRequestBody {
  signed_jwts?: string[];
}

const validateAndParseRequest = (
  request: APIGatewayProxyEvent,
): string => {
  const userId = request.queryStringParameters?.["user_id"];
  return userId;
}