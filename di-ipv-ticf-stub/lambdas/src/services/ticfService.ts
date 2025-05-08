import { v4 as uuid } from "uuid";
import { getSsmParameter } from "../common/ssmParameter";
import TicfRequest from "../domain/ticfRequest";
import ServiceResponse from "../domain/serviceResponse";
import TicfEvidenceItem from "../domain/ticfEvidenceItem";
import TicfVc from "../domain/ticfVc";
import { signJwt } from "./signingService";
import { config } from "../common/config";
import { getUserEvidence } from "../management/services/userEvidenceService";
import UserEvidenceItem from "../management/model/userEvidenceItem";

export async function processGetVCRequest(
  ticfRequest: TicfRequest,
): Promise<ServiceResponse> {
  const ticfSigningKey = await getSsmParameter(
    config.ticfParamBasePath + "signingKey",
  );
  const ticfComponentId = await getSsmParameter(
    config.ticfParamBasePath + "componentId",
  );

  const timestamp = Math.floor(new Date().getTime() / 1000);
  const userEvidenceItem = await getUserEvidenceFromDb(ticfRequest.sub);
  if (userEvidenceItem) {
    console.info("Returning pre-configured response");
  }

  const responseDelay = await getResponseDelay(userEvidenceItem);
  if (responseDelay > 0) {
    console.info(`Response delay configured for ${responseDelay}ms`);
    await new Promise((r) => setTimeout(r, responseDelay));
  }

  const ticfEvidenceItem = userEvidenceItem?.evidence;

  const payload: TicfVc = {
    iss: ticfComponentId,
    sub: ticfRequest.sub,
    aud: ticfComponentId,
    jti: `urn:uuid:${uuid()}`,
    nbf: timestamp,
    iat: timestamp,
    vc: {
      evidence: [
        ticfEvidenceItem || getDefaultEvidenceItem(),
      ],
      type: ["VerifiableCredential", "RiskAssessmentCredential"],
    },
  };

  const returnJwt = await signJwt(payload, ticfSigningKey);

  return {
    response: {
      sub: ticfRequest.sub,
      govuk_signin_journey_id: ticfRequest.govuk_signin_journey_id,
      vtr: ticfRequest.vtr,
      vot: ticfRequest.vot,
      vtm: ticfRequest.vtm,
      "https://vocab.account.gov.uk/v1/credentialJWT": [returnJwt],
    },
    statusCode: userEvidenceItem?.statusCode,
  };
}

function getDefaultEvidenceItem(): TicfEvidenceItem {
  return {
    type: "RiskAssessment",
    txn: uuid(),
  };
}

async function getUserEvidenceFromDb(
  userId: string,
): Promise<UserEvidenceItem | undefined> {
  const userEvidenceItem: UserEvidenceItem | null =
    await getUserEvidence(userId);
  return userEvidenceItem ?? undefined;
}

async function getResponseDelay(userEvidenceItem: UserEvidenceItem | undefined): Promise<number> {
  if (userEvidenceItem?.responseDelay) {
    return userEvidenceItem.responseDelay * 1000;
  }
  const param = await getSsmParameter("defaultResponseDelay");
  return parseInt(param);
}

export default processGetVCRequest;
