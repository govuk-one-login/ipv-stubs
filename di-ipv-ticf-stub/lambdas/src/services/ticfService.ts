import { JWTPayload } from "jose";
import { v4 as uuid } from "uuid";

import { buildSignedJwt } from "stub-oauth-client";
import type { SignedJwtParams } from "stub-oauth-client";

import { getSsmParameter } from "../common/ssmParameter";
import TicfRequest from "../domain/ticfRequest";
import TicfResponse from "../domain/ticfResponse";
import TicfEvidenceItem from "../domain/ticfEvidenceItem";

export class TicfService {
  async processGetVCRequest(ticfRequest: TicfRequest): Promise<TicfResponse> {
    // preparing response
    let ticfSigningKey: string;
    try {
      ticfSigningKey = await getSsmParameter(
        process.env.TICF_PARAM_BASE_PATH + "signingKey"
      );
    } catch (error) {
      throw new Error(`Error while retrieving TicF CRI VC signing key.`);
    }
    let timeoutVC: string | null | undefined = await getSsmParameter(
      process.env.TICF_PARAM_BASE_PATH + "timeoutVC"
    );
    timeoutVC ??= "false";
    let includeCIToVC: string | null | undefined = await getSsmParameter(
      process.env.TICF_PARAM_BASE_PATH + "includeCIToVC"
    );
    includeCIToVC ??= "false";

    const buildJwtParams: SignedJwtParams = {
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
    return {
      sub: ticfRequest.sub,
      govuk_signin_journey_id: ticfRequest.govuk_signin_journey_id,
      vtr: ticfRequest.vtr,
      vot: ticfRequest.vot,
      vtm: ticfRequest.vtm,
      "https://vocab.account.gov.uk/v1/credentialJWT": [returnJwt],
    };
    // return responseBody;
  }
}

function getCustomClaims(
  timeOutVC: boolean,
  includeCIToVC: boolean,
  userId: string
): JWTPayload {
  return {
    sub: userId,
    iss: process.env.ISSUER,
    aud: "https://development-di-ipv-core-front.london.cloudapps.digital",
    nbf: Date.now(),
    vc: {
      evidence: [getEvidenceItem(timeOutVC, includeCIToVC)],
      type: ["VerifiableCredential", "RiskAssessmentCredential"],
    },
    jti: "urn:uuid:" + uuid(),
  };
}

function getEvidenceItem(
  timeOutVC: boolean,
  includeCIToVC: boolean
): TicfEvidenceItem {
  if (timeOutVC) {
    return { type: "RiskAssessment" };
  } else {
    if (includeCIToVC) {
      return {
        type: "RiskAssessment",
        ci: ["V03"],
        txn: uuid(),
      };
    } else {
      return {
        type: "RiskAssessment",
        txn: uuid(),
      };
    }
  }
}

const ticfService: any = new TicfService();

export default ticfService;
