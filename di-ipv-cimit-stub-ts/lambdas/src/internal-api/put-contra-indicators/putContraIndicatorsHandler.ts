import { APIGatewayProxyEvent, APIGatewayProxyResultV2, Context } from "aws-lambda";
import { buildApiResponse, getErrorMessage } from "../../common/apiResponseBuilder";

const FAILURE_RESPONSE = "fail";
const SUCCESS_RESPONSE = "success";


export const putContraIndicatorsHandler = async (
  request: APIGatewayProxyEvent, context: Context
): Promise<APIGatewayProxyResultV2> => {
    return buildApiResponse(200, {result: SUCCESS_RESPONSE});

}