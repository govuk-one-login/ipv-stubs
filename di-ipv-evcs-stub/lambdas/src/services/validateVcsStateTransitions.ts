import { VcDetails } from "../domain/sharedTypes";
import { getStateTransitions } from "../domain/enums";
import { getSignatureFromJwt } from "../common/utils";

export function validateVcsStateTransitions(
  existingVcs: VcDetails[],
  newVcs: VcDetails[],
): void {
  const newVcSignatures = newVcs.map(({ vc }) => getSignatureFromJwt(vc));

  const existingMatchingVcs = existingVcs.filter(({ vc }) =>
    newVcSignatures.includes(getSignatureFromJwt(vc)),
  );

  existingMatchingVcs.forEach((existingMatchingVc) => {
    const currentState = existingMatchingVc.state;
    const newState = newVcs.find(
      (newVc) =>
        getSignatureFromJwt(newVc.vc) ===
        getSignatureFromJwt(existingMatchingVc.vc),
    )?.state;
    if (!newState) throw Error("Not Allowed");

    const allowedTransitions = getStateTransitions(newState);

    if (allowedTransitions.length === 0) {
      return;
    }

    if (!allowedTransitions.includes(currentState)) {
      throw new Error(
        `State VC transition from: ${currentState} to ${newState} is not allowed`,
      );
    }
  });
}
