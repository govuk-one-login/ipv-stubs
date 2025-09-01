import { getVotForUserIdentity } from "../../src/utils/votHelper";

describe("getVotForUserIdentity", () => {
  it.each([
    {
      case: "return the matched vot in requestedVtr",
      sisVot: "P2",
      requestedVtrs: ["P2", "P1"],
      expectedVtr: "P2",
    },
    {
      case: "return P0 if sisVot does not meet any of the requestedVtr",
      sisVot: "P1",
      requestedVtrs: ["P2", "P3"],
      expectedVtr: "P0",
    },
    {
      case: "return highest vot within requestedVtr if no match",
      sisVot: "P3",
      requestedVtrs: ["P2", "P1"],
      expectedVtr: "P2",
    },
    {
      case: "return highest vot within requestedVtr where there are VTRs higher and lower than sisVot",
      sisVot: "P2",
      requestedVtrs: ["P3", "P1"],
      expectedVtr: "P1",
    },
  ])(
    "should return $expectedVtr when $case",
    ({ sisVot, requestedVtrs, expectedVtr }) => {
      // Act
      const res = getVotForUserIdentity(sisVot, requestedVtrs);

      // Assert
      expect(res).toBe(expectedVtr);
    },
  );

  it("should throw if sisVot is invalid", () => {
    // Act/Assert
    expect(() => getVotForUserIdentity("notValidVot", ["P2"])).toThrow(
      new Error("Invalid level of confidence from SIS record"),
    );
  });
});
