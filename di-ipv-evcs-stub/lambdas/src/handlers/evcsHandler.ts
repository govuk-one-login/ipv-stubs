import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { JWTPayload } from "jose";
import { buildApiResponse } from "../common/apiResponses";
import PostRequest from "../domain/postRequest";
import PatchRequest from "../domain/patchRequest";
import ServiceResponse from "../domain/serviceResponse";
import {
  CreateVcStates,
  UpdateVcStates,
  VcState,
} from "../domain/enums/vcState";
import { processPostUserVCsRequest } from "../services/evcsService";
import { processGetUserVCsRequest } from "../services/evcsService";
import { processPatchUserVCsRequest } from "../services/evcsService";
import { verifyTokenAndReturnPayload } from "../services/jwtService";
import { getErrorMessage } from "../common/utils";

export async function createHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info(`---Create Request received----`);
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ message: "Missing userId." }, 400);
  }

  let request;
  try {
    request = parsePostRequest(event);
  } catch (error) {
    console.error(error);
    return buildApiResponse({ message: getErrorMessage(error) }, 400);
  }
  const res = await processPostUserVCsRequest(
    decodeURIComponent(userId),
    request,
  );

  return buildApiResponse(res.response, res.statusCode);
}

export async function updateHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info(`---Update request received----`);
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ message: "Missing userId." }, 400);
  }

  let request;
  try {
    request = parsePatchRequest(event);
  } catch (error) {
    console.error(error);
    return buildApiResponse({ message: getErrorMessage(error) }, 400);
  }
  const res = await processPatchUserVCsRequest(
    decodeURIComponent(userId),
    request,
  );

  return buildApiResponse(res.response, res.statusCode);
}

export async function getHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info(`---Get request received----`);
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ message: "Missing userId." }, 400);
  }
  const decodedUserId = decodeURIComponent(userId);

  try {
    let requestedStates: string[];
    let accessTokenVerified;
    try {
      requestedStates = getRequestedStates(event);

      accessTokenVerified = event.path?.startsWith("/migration")
        ? true
        : await verifyAccessToken(
            validateAccessToken(
              event.headers
                ? event.headers[
                    Object.keys(event.headers).find(
                      (header) => header.toLowerCase() === "authorization",
                    ) || ""
                  ]
                : undefined,
            ),
            decodedUserId,
          );
    } catch (error) {
      console.error(error);
      return buildApiResponse({ message: getErrorMessage(error) }, 400);
    }

    let res: ServiceResponse = {
      response: Object,
    };
    if (accessTokenVerified)
      res = await processGetUserVCsRequest(decodedUserId, requestedStates);

    return buildApiResponse(res.response, res.statusCode);
  } catch (error) {
    console.error(error);
    return buildApiResponse({ message: getErrorMessage(error) }, 500);
  }
}

function parsePostRequest(event: APIGatewayProxyEvent): PostRequest[] {
  console.info(`---Request parsing----`);
  if (!event.body) {
    throw new Error("Missing request body");
  }

  const postRequest = JSON.parse(event.body);
  if (postRequest?.length <= 0 || !isValidCreateVcState(postRequest)) {
    throw new Error("Invalid request");
  }

  return postRequest;
}

function parsePatchRequest(event: APIGatewayProxyEvent): PatchRequest[] {
  console.info(`---Request parsing----`);
  if (!event.body) {
    throw new Error("Missing request body");
  }

  const patchRequest = JSON.parse(event.body);
  if (
    patchRequest?.updateVCs?.length <= 0 ||
    !isValidUpdateVcState(patchRequest)
  ) {
    throw new Error("Invalid request");
  }

  return patchRequest;
}

function isValidCreateVcState(postRequest: PostRequest[]): boolean {
  for (const postRequestItem of postRequest) {
    if (!(postRequestItem.state in CreateVcStates)) {
      return false;
    }
  }
  return true;
}

function isValidUpdateVcState(patchRequest: PatchRequest[]): boolean {
  for (const patchRequestItem of patchRequest) {
    if (!(patchRequestItem.state in UpdateVcStates)) {
      return false;
    }
  }
  return true;
}

function getRequestedStates(event: APIGatewayProxyEvent): string[] {
  const STATE_ALL = "ALL";
  const stateInQuery = decodeURIComponent(
    event.queryStringParameters?.state ?? "",
  );
  let requestStates = Array.from((stateInQuery || VcState.CURRENT).split(","));
  requestStates.forEach(function (state) {
    if (state != STATE_ALL && !(state in VcState)) {
      throw new Error("Invalid state in query param.");
    }
  });
  if (requestStates.length == 1 && requestStates.includes(STATE_ALL)) {
    // all states requested
    requestStates = Object.values(VcState).map((value) => value);
  }
  return requestStates;
}

function validateAccessToken(authheader: string | undefined): string {
  if (authheader === undefined) {
    throw new Error("Request missing access token");
  }
  const parts = authheader!.split(" ");
  if (parts.length != 2) {
    throw new Error("Invalid access token value");
  }
  if (parts[0] != "Bearer") {
    throw new Error("Access token type must be Bearer");
  }
  if (parts[1] === undefined || parts[1] === "") {
    throw new Error("The access token value must contain some value");
  }

  return parts[1];
}

async function verifyAccessToken(
  jwt: string,
  userId: string,
): Promise<boolean> {
  let payload: JWTPayload;
  try {
    payload = await verifyTokenAndReturnPayload(jwt);
  } catch (error) {
    console.error(error);
    throw new Error("Access token verification failed");
  }
  if (userId !== payload.sub) {
    throw new Error(
      "User id doesn't match with `sub` claim value provided in the bearer token",
    );
  }
  return true;
}
