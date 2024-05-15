// Based on https://kentcdodds.com/blog/get-a-catch-block-error-message-with-typescript
type ObjectWithMessage = {
  message: string;
};

function isObjectWithMessage(error: unknown): error is ObjectWithMessage {
  return (
    typeof error === "object" &&
    error !== null &&
    "message" in error &&
    typeof (error as Record<string, unknown>).message === "string"
  );
}

export default function getErrorMessage(error: unknown): string {
  if (isObjectWithMessage(error)) {
    return error.message;
  }

  try {
    return JSON.stringify(error);
  } catch {
    // fallback in case there's an error stringifying the error
    // like with circular references for example.
    return "Couldn't get error message from: " + String(error);
  }
}
