import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { BadRequestError } from "./exceptions";
import { buildApiResponse, getErrorMessage } from "./apiResponseBuilder";
import { getCIsForUserID } from "../../common/dataService";
import { JWTPayload } from "jose";
import { getCimitComponentId } from "../../common/configService";
import { signJWT } from "./jwtSigning";

interface Evidence {
  contraIndicator: ContraIndicator[];
  type: "SecurityCheck";
}

export interface VcClaim {
  evidence: Evidence[];
  type: ["VerifiableCredential", "SecurityCheckCredential"];
}

interface MitigatingCredential {
  issuer: string;
  validFrom: string;
  txn: string;
  id: string;
}

interface Mitigation {
  code: string;
  mitigatingCredential: MitigatingCredential[];
}

export interface ContraIndicator {
  code: string;
  document: string;
  issuanceDate: string;
  issuers: string[];
  mitigation: Mitigation[];
  incompleteMitigation: Mitigation[];
  txn: string[];
}

interface GetContraIndicatorCredentialRequest {
  userId: string;
  govukSigninJourneyId: string;
  ipAddress: string;
}

export interface GetContraIndicatorCredentialResponse {
  vc: string;
}

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
    (ci1, ci2) => ci1.issuanceDate - ci2.issuanceDate,
  );

  const mappedCis = userCis.map((ci) => ({
    code: ci.contraIndicatorCode,
    document: ci.document,
    issuanceDate: new Date(ci.issuanceDate * 1000).toISOString(),
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

  const ciCodeAndDocumentsMatch = (
    ci1: ContraIndicator,
    ci2: ContraIndicator,
  ) => ci1.code === ci2.code && ci1.document === ci2.document;

  // Create list of CIs from userCis
  const deduplicatedCis: ContraIndicator[] = [];
  mappedCis.forEach((parsedCi, idx) => {
    if (idx === 0) {
      deduplicatedCis.push(parsedCi);
    } else {
      deduplicatedCis.forEach((checkedCi) => {
        if (ciCodeAndDocumentsMatch(parsedCi, checkedCi)) {
          checkedCi.issuers.push(parsedCi.issuers[0]);
          checkedCi.mitigation = parsedCi.mitigation;
          checkedCi.issuanceDate = parsedCi.issuanceDate;
          checkedCi.txn = parsedCi.txn;
        } else {
          deduplicatedCis.push(parsedCi);
        }
      });
    }
  });

  return deduplicatedCis;
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
  return {
    sub: userId,
    iss: await getCimitComponentId(),
    nbf: Math.floor(now / 1000),
    exp: now + 60 * 15,
    vc: vcClaim,
  };
};
