export interface InvalidateRequest {
  userId: string;
}

export function isInvalidateRequest(
  value: unknown,
): value is InvalidateRequest {
  if (typeof value !== "object" || value === null) return false;
  const obj = value as Record<string, unknown>;
  return typeof obj.userId === "string";
}
