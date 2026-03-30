import { VcDetails } from "./vcDetails";
import EvcsStoredIdentityItem from "../model/storedIdentityItem";

export default interface ServiceResponse {
  response?: ServiceResponseBody;
  statusCode?: number;
}

interface ServiceResponseBody {
  messageId?: string;
  message?: string;
}

export function createServiceResponse(
  statusCode: number,
  messageId?: string,
): ServiceResponse {
  const responseBody: ServiceResponseBody = {};
  if (messageId) {
    responseBody.messageId = messageId;
  }
  return {
    response: responseBody,
    statusCode,
  };
}

export function createServiceResponseWithBody(
  statusCode: number,
  body: object,
): ServiceResponse {
  return {
    response: body,
    statusCode,
  };
}

export function createServiceResponseWithMessage(
  statusCode: number,
  message: string,
): ServiceResponse {
  return {
    response: { message },
    statusCode,
  };
}

type VcFromGetResponse = Omit<VcDetails, "provenance">;

export interface GetResponse {
  response: {
    vcs: VcFromGetResponse[];
  };
  statusCode: number;
}

export interface GetStoredIdentity {
  storedIdentities: Omit<EvcsStoredIdentityItem, "metadata">[];
}
