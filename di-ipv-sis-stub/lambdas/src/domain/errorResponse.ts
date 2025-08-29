interface ErrorResponse {
  error: string;
  error_description: string;
}

export const buildBadRequestResponse = (desc: string): ErrorResponse => ({
  error: "Bad Request",
  error_description: desc,
});

export const buildNotFoundResponse = (): ErrorResponse => ({
  error: "not_found",
  error_description: "No stored identity exists for this user",
});

export const buildUnauthorisedResponse = (): ErrorResponse => ({
  error: "invalid_token",
  error_description: "Bearer token is missing or invalid",
});

export const buildForbiddenResponse = (): ErrorResponse => ({
  error: "forbidden",
  error_description: "Access token expired or not permitted",
});

export const buildServerErrorResponse = (): ErrorResponse => ({
  error: "server_error",
  error_description: "Temporary SIS outage",
});
