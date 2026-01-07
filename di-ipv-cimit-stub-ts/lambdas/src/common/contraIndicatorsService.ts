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
import { persistCimitStubItem } from "./cimitStubItemService";

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
    const ci = vc.evidence[0].ci;
    if (!ci || ci.length === 0) {
      console.info("No CI in evidence");
      return;
    }

    const issuanceDate = signedJwt.nbf
      ? new Date(signedJwt.nbf * 1000)
      : new Date();
    const issuanceDateString = issuanceDate.toISOString();

    const dbItems = ci.map((ci) => {
      const code = ci.toUpperCase();
      return {
        userId: signedJwt.sub || "",
        contraIndicatorCode: code,
        issuer: signedJwt.iss || "",
        issuanceDate: issuanceDateString,
        mitigations: [],
        document: getDocument(vc),
        txn: vc.evidence[0].txn || "",
        ttl: 0, // TODO: This and sortKey are set in persistCimitStubItem so we don't need to set them here.
        sortKey: "",
      };
    });

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
