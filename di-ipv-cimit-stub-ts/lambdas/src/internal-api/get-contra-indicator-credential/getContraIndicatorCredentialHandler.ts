import {APIGatewayProxyEvent, APIGatewayProxyResultV2} from "aws-lambda";
import {BadRequestError} from "./exceptions";
import {buildApiResponse, getErrorMessage} from "./apiResponseBuilder";
import {getCIsForUserID} from "../../common/dataService";
import { JWTPayload, SignJWT, importPKCS8 } from "jose";
import { getCimitComponentId, getCimitSigningKey } from "../../common/configService";

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

interface ContraIndicator {
  code: string;
  document: string;
  issuanceDate: string;
  issuers: Set<string>;
  mitigation: Mitigation[];
  incompleteMitigation: Mitigation[];
  txn: string[]
}

interface GetContraIndicatorCredentialRequest {
  userId: string;
  govukSigninJourneyId: string;
  ipAddress: string;
}

export interface GetContraIndicatorCredentialResponse {
  vc: string;
}

export const getContraIndicatorCredentialHandler = async (request: APIGatewayProxyEvent): Promise<APIGatewayProxyResultV2> => {
  console.info("Function invoked:", "GetContraIndicatorCredential");
  try {
    const parsedRequest = validateAndParseRequest(request);
    const userCis = await getCIsForUserID(parsedRequest.userId);

    const parsedCis: ContraIndicator[] = userCis.map(ci => ({
      code: ci.contraIndicatorCode,
      document: ci.document,
      issuanceDate: new Date(ci.issuanceDate).toISOString(),
      issuers: new Set([ci.issuer]),
      mitigation: ci.mitigations.map(mitigationCode => ({
        code: mitigationCode,
        mitigatingCredential: [{
          issuer: "",
          validFrom: "",
          txn: "",
          id: ""
        }]
      })),
      incompleteMitigation: [],
      txn: [ci.txn]
    }))

    console.log(parsedCis);

    const vcClaim: VcClaim = {
      evidence: [
        { contraIndicator: parsedCis, type: "SecurityCheck"}
      ],
      type: ["VerifiableCredential", "SecurityCheckCredential"]
    }

    const now = Date.now();
    const claimsSet: JWTPayload = {
      sub: parsedRequest.userId,
      iss: await getCimitComponentId(),
      nbf: now,
      exp: now + (60*15*1000),
      vc: vcClaim
    }
    const formattedSigningKey = await importPKCS8(
      `-----BEGIN PRIVATE KEY-----\n${await getCimitSigningKey()}\n-----END PRIVATE KEY-----`, // pragma: allowlist secret
      "ES256",
    );
    const signedJwt = await new SignJWT(claimsSet)
      .setProtectedHeader({ alg: "ES256", typ: "JWT" })
      .sign(formattedSigningKey);

    // generate VC claim

    return buildApiResponse(200, {vc: signedJwt});
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
}

const validateAndParseRequest = (request: APIGatewayProxyEvent): GetContraIndicatorCredentialRequest => {
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
    ipAddress
  }
}