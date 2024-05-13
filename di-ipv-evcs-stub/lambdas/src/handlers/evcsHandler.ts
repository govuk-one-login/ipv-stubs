import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponses";
import PostRequest from "../domain/postRequest";
import PatchRequest from "../domain/patchRequest";
import ServiceResponse from "../domain/serviceResponse";
import PersistVC from "../domain/persistVC";
import UpdateVC from "../domain/updateVC";
import { CreateVcStates, UpdateVcStates } from "../domain/enums/vcState";

import { processPostUserVCsRequest } from "../services/evcsService";
import { processGetUserVCsRequest } from "../services/evcsService";
import { processPatchUserVCsRequest } from "../services/evcsService";
import { verifyToken } from "../services/jwtService";

export async function createHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info(`---Create Request received----`);
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ errorMessage: "Missing userId." }, 400);
  }

  try {
    let request;
    try {
      request = parsePostRequest(event);
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (error: any) {
      console.error(error);
      return buildApiResponse({ errorMessage: error.message }, 400);
    }
    const res = await processPostUserVCsRequest(
      decodeURIComponent(userId),
      request,
    );

    return buildApiResponse(res.response, res.statusCode);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 500);
  }
}

export async function updateHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info(`---Request received----`);
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ errorMessage: "Missing userId." }, 400);
  }

  try {
    let request;
    try {
      request = parsePatchRequest(event);
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (error: any) {
      console.error(error);
      return buildApiResponse({ errorMessage: error.message }, 400);
    }
    const res = await processPatchUserVCsRequest(
      decodeURIComponent(userId),
      request,
    );

    return buildApiResponse(res.response, res.statusCode);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 500);
  }
}

export async function getHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info(`---Request received----`);
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ errorMessage: "Missing userId." }, 400);
  }

  try {
    let accessTokenVerified;
    let res: ServiceResponse = {
      response: Object,
    };
    try {
      accessTokenVerified = await verifyAccessToken(
        validateAccessToken(event.headers?.Authorisation),
      );
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (error: any) {
      console.error(error);
      return buildApiResponse({ errorMessage: error.message }, 400);
    }

    if (accessTokenVerified)
      res = await processGetUserVCsRequest(decodeURIComponent(userId));

    return buildApiResponse(res.response, res.statusCode);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 500);
  }
}

function parsePostRequest(event: APIGatewayProxyEvent): PostRequest {
  console.info(`---Request parsing----`);
  if (!event.body) {
    throw new Error("Missing request body");
  }

  const postRequest = JSON.parse(event.body);
  if (
    !postRequest ||
    !postRequest.persistVCs ||
    postRequest.persistVCs.length <= 0 ||
    !isValidCreateVcState(postRequest.persistVCs)
  ) {
    throw new Error("Invalid request");
  }

  return postRequest;
}

function parsePatchRequest(event: APIGatewayProxyEvent): PatchRequest {
  console.info(`---Request parsing----`);
  if (!event.body) {
    throw new Error("Missing request body");
  }

  const patchRequest = JSON.parse(event.body);
  if (
    !patchRequest ||
    !patchRequest.updateVCs ||
    patchRequest.updateVCs.length <= 0 ||
    !isValidUpdateVcState(patchRequest.updateVCs)
  ) {
    throw new Error("Invalid request");
  }

  return patchRequest;
}

function isValidCreateVcState(persistVCs: PersistVC[]): boolean {
  for (const persistVC of persistVCs) {
    if (!(persistVC.state in CreateVcStates)) {
      return false;
    }
  }
  return true;
}

function isValidUpdateVcState(updateVCs: UpdateVC[]): boolean {
  for (const updateVC of updateVCs) {
    if (!(updateVC.state in UpdateVcStates)) {
      return false;
    }
  }
  return true;
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

async function verifyAccessToken(jwt: string): Promise<boolean> {
  try {
    return await verifyToken(jwt);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    throw new Error("The access token varification failed");
  }
}
