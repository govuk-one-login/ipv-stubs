import { v4 as uuid } from "uuid";
import { getSsmParameter } from "../common/ssmParameter";
import TicfRequest from "../domain/ticfRequest";
import TicfResponse from "../domain/ticfResponse";
import TicfEvidenceItem from "../domain/ticfEvidenceItem";
import TicfVc from "../domain/ticfVc";
import { signJwt } from "./signingService";

export async function processGetVCRequest(
  ticfRequest: TicfRequest
): Promise<TicfResponse> {
  const ticfSigningKey = await getSsmParameter(process.env.TICF_PARAM_BASE_PATH + "signingKey");
  const ticfComponentId = await getSsmParameter(process.env.TICF_PARAM_BASE_PATH + "componentId");
  const timeoutVc = await getSsmParameter(process.env.TICF_PARAM_BASE_PATH + "timeoutVC") === 'true';
  const includeCi = await getSsmParameter(process.env.TICF_PARAM_BASE_PATH + "includeCIToVC") === 'true';

  const timestamp = Math.floor(new Date().getTime() / 1000);

  const payload: TicfVc = {
    iss: ticfComponentId,
    sub: ticfRequest.sub,
    aud: ticfComponentId,
    jti: `urn:uuid:${uuid()}`,
    nbf: timestamp,
    iat: timestamp,
    vc: {
      evidence: [getEvidenceItem(timeoutVc, includeCi)],
      type: ["VerifiableCredential", "RiskAssessmentCredential"],
    },
  };

  const returnJwt = await signJwt(payload, ticfSigningKey);
  return {
    sub: ticfRequest.sub,
    govuk_signin_journey_id: ticfRequest.govuk_signin_journey_id,
    vtr: ticfRequest.vtr,
    vot: ticfRequest.vot,
    vtm: ticfRequest.vtm,
    "https://vocab.account.gov.uk/v1/credentialJWT": [returnJwt],
  };
}

function getEvidenceItem(
  timeoutVc: boolean,
  includeCi: boolean
): TicfEvidenceItem {
  if (timeoutVc) {
    return { type: "RiskAssessment" };
  } else {
    if (includeCi) {
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

export default processGetVCRequest;