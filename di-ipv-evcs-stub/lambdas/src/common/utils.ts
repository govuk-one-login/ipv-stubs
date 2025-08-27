import { getSsmParameter } from "./ssmParameter";
import { config } from "./config";

export function getErrorMessage(error: unknown) {
  if (error instanceof Error) return error.message;
  return String(error);
}

export async function getTtl(): Promise<number> {
  const evcsTtlSeconds: number = parseInt(
    await getSsmParameter(config.evcsParamBasePath + "evcsStubTtl"),
  );
  return Math.floor(Date.now() / 1000) + evcsTtlSeconds;
}
