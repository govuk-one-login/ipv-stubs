import { getVotForUserIdentity } from "../../src/utils/votHelper";

describe("getVotForUserIdentity", () => {
  it.each([
    {
      case: "return the matched vot in requestedVtr",
      sisVot: "P2",
      requestedVtrs: ["P2", "P1"],
      isValid: true,
      expectedVtr: "P2",
    },
    {
      case: "return P0 if sisVot does not meet any of the requestedVtr",
      sisVot: "P1",
      requestedVtrs: ["P2", "P3"],
      isValid: true,
      expectedVtr: "P0",
    },
    {
      case: "return highest vot within requestedVtr if no match",
      sisVot: "P3",
      requestedVtrs: ["P2", "P1"],
      isValid: true,
      expectedVtr: "P2",
    },
    {
      case: "return single requesterVtr if sisVot is stronger",
      sisVot: "P3",
      requestedVtrs: ["P2"],
      isValid: true,
      expectedVtr: "P2",
    },
    {
      case: "return highest vot within requestedVtr where there are VTRs higher and lower than sisVot",
      sisVot: "P2",
      requestedVtrs: ["P3", "P1"],
      isValid: true,
      expectedVtr: "P1",
    },
    {
      case: "return sisVot if it matches lowest strength requested vtr",
      sisVot: "P1",
      requestedVtrs: ["P3", "P1"],
      isValid: true,
      expectedVtr: "P1",
    },
    {
      case: "return P0 if SIS record is invalid",
      sisVot: "P2",
      requestedVtrs: ["P3", "P1"],
      isValid: false,
      expectedVtr: "P0",
    },
  ])(
    "should return $expectedVtr when $case",
    ({ sisVot, requestedVtrs, expectedVtr, isValid }) => {
      // Act
      const res = getVotForUserIdentity(sisVot, requestedVtrs, isValid);

      // Assert
      expect(res).toBe(expectedVtr);
    },
  );

  it("should throw if sisVot is invalid", () => {
    // Act/Assert
    expect(() => getVotForUserIdentity("notValidVot", ["P2"], true)).toThrow(
      new Error("Invalid level of confidence from SIS record"),
    );
  });
});
