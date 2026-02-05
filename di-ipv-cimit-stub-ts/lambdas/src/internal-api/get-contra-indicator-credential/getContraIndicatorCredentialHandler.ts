import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { BadRequestError } from "./exceptions";
import {
  buildApiResponse,
  getErrorMessage,
} from "../../common/apiResponseBuilder";
import { getCIsForUserID } from "../../common/dataService";
import { JWTPayload } from "jose";
import { getCimitComponentId } from "../../common/configService";
import { signJWT } from "./jwtSigning";
import {
  GetContraIndicatorCredentialRequest,
  ContraIndicator,
  VcClaim,
} from "../../common/contraIndicatorTypes";

export const getContraIndicatorCredentialHandler = async (
  request: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> => {
  console.info("Function invoked:", "GetContraIndicatorCredential");
  try {
    const parsedRequest = validateAndParseRequest(request);
    const contraIdicators = await getCIs(parsedRequest.userId);
    const claimsSet = await makeJWTPayload(
      contraIdicators,
      parsedRequest.userId,
    );
    const signedJwt = await signJWT(claimsSet);

    return buildApiResponse(200, { vc: signedJwt });
  } catch (error) {
    console.error(getErrorMessage(error));

    if (error instanceof BadRequestError) {
      return buildApiResponse(400, {
        message: getErrorMessage(error),
      });
    }

    return buildApiResponse(500, {
      message: getErrorMessage(error),
    });
  }
};

const validateAndParseRequest = (
  request: APIGatewayProxyEvent,
): GetContraIndicatorCredentialRequest => {
  const userId = request.queryStringParameters?.["user_id"];
  if (!userId) {
    console.error("Missing userId from request");
    throw new BadRequestError("Missing userId from request");
  }

  const govukSigninJourneyId = request.headers["govuk-signin-journey-id"];
  if (!govukSigninJourneyId) {
    console.error("Missing govukSigninJourneyId from request");
    throw new BadRequestError("Missing govukSigninJourneyId from request");
  }

  const ipAddress = request.headers["ip-address"];
  if (!ipAddress) {
    console.error("Missing ipAddress from request");
    throw new BadRequestError("Missing ipAddress from request");
  }

  return {
    userId,
    govukSigninJourneyId,
    ipAddress,
  };
};

const getCIs = async (userId: string): Promise<ContraIndicator[]> => {
  const userCis = (await getCIsForUserID(userId)).sort(
    (ci1, ci2) =>
      new Date(ci1.issuanceDate).getTime() -
      new Date(ci2.issuanceDate).getTime(),
  );
  const mappedCis = userCis.map((ci) => ({
    code: ci.contraIndicatorCode,
    document: ci.document,
    issuanceDate: ci.issuanceDate,
    issuers: [...new Set([ci.issuer])],
    mitigation: ci.mitigations.map((mitigationCode) => ({
      code: mitigationCode,
      mitigatingCredential: [
        {
          issuer: "",
          validFrom: "",
          txn: "",
          id: "",
        },
      ],
    })),
    incompleteMitigation: [],
    txn: [ci.txn],
  }));

  return ciDeduplicator(mappedCis);
};

const ciDeduplicator = (mappedCis: ContraIndicator[]): ContraIndicator[] => {
  const cisAreFunctionallySame = (ci1: ContraIndicator, ci2: ContraIndicator) =>
    ci1.code === ci2.code && ci1.document === ci2.document;

  if (mappedCis.length === 0) return [];

  const distinctCIs = mappedCis.filter(
    (ci, i, self) =>
      i ===
      self.findIndex((ciToCompare) => cisAreFunctionallySame(ci, ciToCompare)),
  );
  const cisThatMatchADistinctOne = mappedCis.filter(
    (ci, i, self) =>
      i !==
      self.findIndex((ciToCompare) => cisAreFunctionallySame(ci, ciToCompare)),
  );

  cisThatMatchADistinctOne.forEach((duplicateCI) => {
    distinctCIs.forEach((distinctCI) => {
      if (cisAreFunctionallySame(duplicateCI, distinctCI)) {
        distinctCI.issuers.push(duplicateCI.issuers[0]);
        distinctCI.mitigation = duplicateCI.mitigation;
        distinctCI.issuanceDate = duplicateCI.issuanceDate;
        distinctCI.txn = duplicateCI.txn;
      }
    });
  });

  return distinctCIs;
};

const makeJWTPayload = async (
    contraIndicators: ContraIndicator[],
    userId: string,
): Promise<JWTPayload> => {
  const vcClaim: VcClaim = {
    evidence: [{ contraIndicator: contraIndicators, type: "SecurityCheck" }],
    type: ["VerifiableCredential", "SecurityCheckCredential"],
  };
  const now = Date.now();
  const secondsUntilExpiry = 60 * 15;
  return {
    sub: userId,
    iss: await getCimitComponentId(),
    nbf: Math.floor(now / 1000),
    exp: now + secondsUntilExpiry,
    vc: vcClaim,
  };
};
