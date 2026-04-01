import { VcDetails } from "./vcDetails";
import EvcsStoredIdentityItem from "../model/storedIdentityItem";

export default interface ServiceResponse {
  response?: ServiceResponseBody;
  statusCode: number;
}

interface ServiceResponseBody {
  messageId?: string;
  message?: string;
}

export function createServiceResponseWithMessageId(
  statusCode: number,
  messageId: string,
): ServiceResponse {
  return createServiceResponse(statusCode, { messageId });
}

export function createServiceResponse(
  statusCode: number,
  body?: object,
): ServiceResponse {
  if (body) {
    return {
      response: body,
      statusCode,
    };
  }
  return {
    statusCode,
  };
}

export function createServiceResponseWithMessage(
  statusCode: number,
  message: string,
): ServiceResponse {
  return createServiceResponse(statusCode, { message });
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
