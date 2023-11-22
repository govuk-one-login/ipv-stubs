import { Handler } from "aws-lambda";

import { buildSignedJwt } from "di-stub-oauth-client";
import type { SignedJwtParams } from "di-stub-oauth-client";

import { JWTPayload } from "jose";
import { v4 as uuid } from "uuid";

import { getSsmParameter } from "../common/ssmParameter";
import { apiResponses } from "../common/apiResponses";
import TicfRequest from "../domain/ticfRequest";
import TicfResponse from "../domain/ticfResponse";

export const handler: Handler = async (event: any) => {
  let ticfRequest: TicfRequest;
  try {
    ticfRequest = JSON.parse(event.body);
  } catch (error) {
    return apiResponses._400();
  }
  if (ticfRequest == null)
    return apiResponses._400({ errorMessage: "Pls. pass proper request." });

  // preparing response
  let ticfSigningKey: string;
  try {
    ticfSigningKey = await getSsmParameter(
      process.env.TICF_PARAM_BASE_PATH + "signingKey"
    );
  } catch (error) {
    return apiResponses._500({
      errorMessage: "Error while retrieving TicF CRI VC signing key.",
    });
  }
  let timeoutVC: string | null | undefined = await getSsmParameter(
    process.env.TICF_PARAM_BASE_PATH + "timeoutVC"
  );
  timeoutVC ??= "false";
  let includeCIToVC: string | null | undefined = await getSsmParameter(
    process.env.TICF_PARAM_BASE_PATH + "includeCIToVC"
  );
  includeCIToVC ??= "false";

  let buildJwtParams: SignedJwtParams;
  buildJwtParams = {
    issuer: process.env.ISSUER,
    customClaims: getCustomClaims(
      JSON.parse(timeoutVC.toLowerCase()),
      JSON.parse(includeCIToVC.toLowerCase()),
      ticfRequest.sub
    ),
    privateSigningKey: ticfSigningKey!,
  };

  // preparing response
  const returnJwt = await buildSignedJwt(buildJwtParams);
  let responseBody: TicfResponse;
  responseBody = {
    sub: ticfRequest.sub,
    govuk_signin_journey_id: ticfRequest.govuk_signin_journey_id,
    vtr: ticfRequest.vtr,
    vot: ticfRequest.vot,
    vtm: ticfRequest.vtm,
    "https://vocab.account.gov.uk/v1/credentialJWT": [returnJwt],
  };

  return apiResponses._200(responseBody);
};

function getCustomClaims(
  timeOutVC: boolean,
  includeCIToVC: boolean,
  userId: string
): JWTPayload {
  if (timeOutVC) {
    return {
      sub: userId,
      iss: process.env.ISSUER,
      aud: "https://development-di-ipv-core-front.london.cloudapps.digital",
      nbf: Date.now(),
      vc: {
        evidence: [
          {
            type: "RiskAssessment",
          },
        ],
        type: ["VerifiableCredential", "RiskAssessmentCredential"],
      },
      jti: "urn:uuid:" + uuid(),
    };
  } else {
    if (includeCIToVC) {
      return {
        sub: userId,
        iss: process.env.ISSUER,
        aud: "https://development-di-ipv-core-front.london.cloudapps.digital",
        nbf: Date.now(),
        vc: {
          evidence: [
            {
              type: "RiskAssessment",
              ci: ["V03"],
              txn: uuid(),
            },
          ],
          type: ["VerifiableCredential", "RiskAssessmentCredential"],
        },
        jti: "urn:uuid:" + uuid(),
      };
    } else {
      return {
        sub: userId,
        iss: process.env.ISSUER,
        aud: "https://development-di-ipv-core-front.london.cloudapps.digital",
        nbf: Date.now(),
        vc: {
          evidence: [
            {
              type: "RiskAssessment",
              txn: uuid(),
            },
          ],
          type: ["VerifiableCredential", "RiskAssessmentCredential"],
        },
        jti: "urn:uuid:" + uuid(),
      };
    }
  }
}
