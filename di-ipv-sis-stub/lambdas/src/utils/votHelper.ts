const VOTS_BY_ASCENDING_STRENGTH = ["P0", "P1", "P2", "P3", "P4"];

export const getVotForUserIdentity = (
  sisVot: string,
  requestedVtrs: string[],
  isSisRecordValid: boolean,
) => {
  if (!isSisRecordValid) {
    return "P0";
  }

  const sisVotIndex = VOTS_BY_ASCENDING_STRENGTH.indexOf(sisVot);

  if (sisVotIndex < 0) {
    throw new Error("Invalid level of confidence from SIS record");
  }

  const validProfiles = requestedVtrs.filter((vot) => {
    const idx = VOTS_BY_ASCENDING_STRENGTH.indexOf(vot);
    return idx >= 0 && idx <= sisVotIndex;
  });

  if (validProfiles.length === 0) {
    return "P0";
  }

  validProfiles.sort(
    (a, b) =>
      VOTS_BY_ASCENDING_STRENGTH.indexOf(b) -
      VOTS_BY_ASCENDING_STRENGTH.indexOf(a),
  );
  return validProfiles[0];
};
