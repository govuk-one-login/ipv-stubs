import { describe, it, expect, vi, beforeEach } from "vitest";
import * as userService from "../../../../src/external-api/stub-management/service/userService";
import * as cimitStubItemService from "../../../../src/common/cimitStubItemService";
import { CimitStubItem } from "../../../../src/common/contraIndicatorTypes";

vi.mock("../../../../src/common/configService", async (importOriginal) => {
  const actual =
    await importOriginal<
      typeof import("../../../../src/common/configService")
    >();
  return {
    ...actual,
    getCimitStubTtl: vi.fn().mockResolvedValue(1800),
  };
});

vi.mock(
  "../../../../src/common/cimitStubItemService",
  async (importOriginal) => {
    const actual =
      await importOriginal<
        typeof import("../../../../src/common/cimitStubItemService")
      >();
    return {
      ...actual,
      persistCimitStubItem: vi.fn(),
      getCIsForUserId: vi.fn(),
      deleteCimitStubItem: vi.fn(),
    };
  },
);

describe("userService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("addUserCis", () => {
    it("should add new user CIs", async () => {
      const userId = "user123";
      const userCisRequests: userService.UserCisRequest[] = [
        {
          code: "code1",
          issuanceDate: "2023-07-25T10:00:00Z",
          issuer: "https://issuer.example.com",
          mitigations: ["V01", "V03"],
          document: "document/this/that",
          txn: "",
        },
        {
          code: "code2",
          issuanceDate: "2023-07-25T10:00:00Z",
          issuer: "https://issuer.example.com",
          mitigations: [],
          document: "",
          txn: "",
        },
      ];

      await userService.addUserCis(userId, userCisRequests);

      expect(cimitStubItemService.persistCimitStubItem).toHaveBeenCalledTimes(
        2,
      );
      expect(cimitStubItemService.persistCimitStubItem).toHaveBeenCalledWith(
        expect.objectContaining({
          userId,
          contraIndicatorCode: "CODE1",
          issuanceDate: "2023-07-25T10:00:00Z",
          issuer: "https://issuer.example.com",
          mitigations: ["V01", "V03"],
          document: "document/this/that",
        }),
      );
      expect(cimitStubItemService.persistCimitStubItem).toHaveBeenCalledWith(
        expect.objectContaining({
          userId,
          contraIndicatorCode: "CODE2",
          issuanceDate: "2023-07-25T10:00:00Z",
          issuer: "https://issuer.example.com",
          mitigations: [],
          document: "",
        }),
      );
    });
  });

  describe("updateUserCis", () => {
    it("should delete and add CIs when updating user CIs", async () => {
      const userId = "user123";
      const userCisRequest: userService.UserCisRequest = {
        code: "code1",
        issuer: "https://issuer.example.com",
        issuanceDate: "2023-07-25T10:00:00Z",
        mitigations: ["V01"],
        document: "",
        txn: "",
      };

      const existingItems: CimitStubItem[] = [
        {
          userId,
          contraIndicatorCode: "CODE1",
          issuanceDate: new Date().toISOString(),
          issuer: "",
          mitigations: [],
          document: "",
          txn: "",
          sortKey: "CODE1#" + new Date().toISOString(),
          ttl: 30000,
        },
      ];

      vi.mocked(cimitStubItemService.getCIsForUserId).mockResolvedValue(
        existingItems,
      );

      await userService.updateUserCis(userId, [userCisRequest]);

      expect(cimitStubItemService.deleteCimitStubItem).toHaveBeenCalledWith(
        userId,
        existingItems[0].sortKey,
      );
      expect(cimitStubItemService.persistCimitStubItem).toHaveBeenCalledWith(
        expect.objectContaining({
          userId,
          contraIndicatorCode: "CODE1",
          issuanceDate: "2023-07-25T10:00:00Z",
          issuer: "https://issuer.example.com",
          mitigations: ["V01"],
        }),
      );
    });
  });
});
