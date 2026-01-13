import { PutContraIndicatorRequest } from "../internal-api/put-contra-indicators/putContraIndicatorsHandler";
import { getErrorMessage } from "./apiResponseBuilder";
import { decodeJwt } from "jose";
import {
  DrivingPermitDetailsClass,
  IdentityCheckCredentialClass,
  PassportDetailsClass,
  ResidencePermitDetailsClass,
  SocialSecurityRecordDetailsClass,
} from "@govuk-one-login/data-vocab/credentials";
import { createStubItem, persistCimitStubItem } from "./cimitStubItemService";
import { CimitStubItem } from "./contraIndicatorTypes";

export const addUserCIs = async (
  request: PutContraIndicatorRequest,
): Promise<void> => {
  try {
    const signedJwt = decodeJwt(request.signed_jwt);
    const vc = signedJwt.vc as IdentityCheckCredentialClass;
    if (!vc) {
      console.info("No VC claim found in jwt");
      return;
    }
    if (!vc.evidence || vc.evidence.length === 0) {
      console.info("No evidence in VC");
      return;
    }
    const cis = vc.evidence[0].ci;
    if (!cis || cis.length === 0) {
      console.info("No CI in evidence");
      return;
    }

    const issuanceDate = signedJwt.nbf
      ? new Date(signedJwt.nbf * 1000)
      : new Date();
    const issuanceDateString = issuanceDate.toISOString();

    const dbItems: CimitStubItem[] = [];
    for (const ci of cis) {
      const item = await createStubItem(
        signedJwt.sub,
        ci.toUpperCase(),
        signedJwt.iss,
        issuanceDateString,
        [],
        getDocument(vc),
        vc.evidence[0].txn,
      );
      dbItems.push(item);
    }

    for (const item of dbItems) {
      await persistCimitStubItem(item);
    }
  } catch (error) {
    console.error(getErrorMessage(error));
    throw new Error("Failed to add user CIs");
  }
};

const getDocument = (vc: IdentityCheckCredentialClass): string => {
  const credentialSubject = vc.credentialSubject;
  if (!credentialSubject) return "";
  if (
    !!credentialSubject.drivingPermit &&
    credentialSubject.drivingPermit.length > 0
  ) {
    const drivingPermit: DrivingPermitDetailsClass =
      credentialSubject.drivingPermit[0];
    return `drivingPermit/GB/${drivingPermit.issuedBy}/${drivingPermit.personalNumber}/${drivingPermit.issueDate}`;
  }
  if (!!credentialSubject.passport && credentialSubject.passport.length > 0) {
    const passport: PassportDetailsClass = credentialSubject.passport[0];
    return `passport/${passport.icaoIssuerCode}/${passport.documentNumber}`;
  }
  if (
    !!credentialSubject.residencePermit &&
    credentialSubject.residencePermit.length > 0
  ) {
    const residencePermit: ResidencePermitDetailsClass =
      credentialSubject.residencePermit[0];
    return `residencePermit/${residencePermit.icaoIssuerCode}/${residencePermit.documentNumber}`;
  }
  if (
    !!credentialSubject.socialSecurityRecord &&
    credentialSubject.socialSecurityRecord.length > 0
  ) {
    const socialSecurityRecord: SocialSecurityRecordDetailsClass =
      credentialSubject.socialSecurityRecord[0];
    return `socialSecurity/GB/${socialSecurityRecord.personalNumber}`;
  }
  return "";
};
