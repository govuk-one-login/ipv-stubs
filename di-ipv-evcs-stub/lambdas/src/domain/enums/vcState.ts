export enum VcState {
  CURRENT = "CURRENT",
  PENDING = "PENDING",
  VERIFICATION = "VERIFICATION",
  ABANDONED = "ABANDONED",
  PENDING_RETURN = "PENDING_RETURN",
  HISTORIC = "HISTORIC",
  VERIFICATION_ARCHIVED = "VERIFICATION_ARCHIVED",
}

export const CreateVcStates = {
  [VcState.CURRENT]: VcState.CURRENT,
  [VcState.PENDING]: VcState.PENDING,
  [VcState.PENDING_RETURN]: VcState.PENDING_RETURN,
  [VcState.VERIFICATION]: VcState.VERIFICATION,
} as const;

export const UpdateVcStates = {
  [VcState.CURRENT]: VcState.CURRENT,
  [VcState.ABANDONED]: VcState.ABANDONED,
  [VcState.PENDING_RETURN]: VcState.PENDING_RETURN,
  [VcState.HISTORIC]: VcState.HISTORIC,
  [VcState.VERIFICATION_ARCHIVED]: VcState.VERIFICATION_ARCHIVED,
} as const;

// Keys are target states, values are lists of states allowed to transition to those target states
export const stateTransitions: Record<VcState, VcState[]> = {
  ABANDONED: [VcState.PENDING, VcState.PENDING_RETURN, VcState.VERIFICATION],
  CURRENT: [VcState.PENDING_RETURN, VcState.PENDING],
  HISTORIC: [VcState.CURRENT],
  PENDING: [],
  PENDING_RETURN: [VcState.PENDING],
  VERIFICATION: [],
  VERIFICATION_ARCHIVED: [VcState.VERIFICATION],
};
