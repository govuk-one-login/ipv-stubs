// This mock has to be set up before we import anything that might use config.
jest.mock("../../src/common/configService", () => ({
  getCimitSigningKey: jest.fn().mockResolvedValue("mock-signing-key"),
  getCimitComponentId: jest.fn().mockResolvedValue("mock-component-id"),
  getCimitStubTableName: jest.fn().mockReturnValue("mock-table"),
  getCimitStubTtl: jest.fn().mockResolvedValue(1000),
  isRunningLocally: false,
}));

import * as cimitStubItemService from "../../src/common/cimitStubItemService";
import { addUserCIs } from "../../src/common/contraIndicatorsService";
import { PutContraIndicatorRequest } from "../../src/internal-api/put-contra-indicators/putContraIndicatorsHandler";

const SIGNED_CRI_VC_WITH_ONE_CI =
  "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDpjMjNlYzE2Ni0yYzMyLTRmMDAtYmRmZS1iMjkzOThlMzY4MDEiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTIyNjc2NTMsImlzcyI6Imh0dHBzOlwvXC9rYnYtY3JpLnN0dWJzLmFjY291bnQuZ292LnVrIiwidmMiOnsidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIklkZW50aXR5Q2hlY2tDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidHlwZSI6IkdpdmVuTmFtZSIsInZhbHVlIjoiTWFyeSJ9LHsidHlwZSI6IkZhbWlseU5hbWUiLCJ2YWx1ZSI6IldhdHNvbiJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTkzMi0wMi0yNSJ9XSwiYWRkcmVzcyI6W3siYnVpbGRpbmdOYW1lIjoiMjIxQiIsInN0cmVldE5hbWUiOiJCQUtFUiBTVFJFRVQiLCJwb3N0YWxDb2RlIjoiTlcxIDZYRSIsImFkZHJlc3NMb2NhbGl0eSI6IkxPTkRPTiIsInZhbGlkRnJvbSI6IjE4ODctMDEtMDEifV19LCJldmlkZW5jZSI6W3sidmVyaWZpY2F0aW9uU2NvcmUiOjAsImNpIjpbIlYwMyJdLCJ0eG4iOiIxOGZiZmU5My0yZTcxLTQ0YmItODhjNS0wZjdkZTYwZmJlODAiLCJ0eXBlIjoiSWRlbnRpdHlDaGVjayJ9XX0sImp0aSI6Ijg2ZTc3NmQxLTVjNmMtNDIzYy05OWNmLWUyZjYxOTQyYzY0YiJ9.TO4mRYGbD9QPxI3W8_gKmB87qTcIehhWXQ2RQgPvWrVbYynai0JDuphYRclXraLIBOAh_XK2mtBCpFnK9Rj0OQ"; // pragma: allowlist secret
const SIGNED_CRI_VC_NO_EVIDENCE =
  "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lkZW50aXR5LnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJpYXQiOjE2ODgxMjM0NjYsIm5iZiI6MTY4ODEyMzQ2NiwiZXhwIjoyMDAzNDgzNDY2LCJzdWIiOiJhLXVzZXItaWQiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiU2VjdXJpdHlDaGVja0NyZWRlbnRpYWwiXSwiZXZpZGVuY2UiOltdfX0.licS4NM0EWKQm6fYT1plBQV6Bk4e9qrdXQ1NOo-GIvmTUhPbRSXHdUvGHUNbnVFxFZMyxdtBM_lkEUfqTpY64A"; // pragma: allowlist secret
const SIGNED_CRI_VC_INVALID_EVIDENCE =
  "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lkZW50aXR5LnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJpYXQiOjE2ODgxMjUwNDAsIm5iZiI6MTY4ODEyNTAzOSwiZXhwIjoyMDAzNDg1MDM5LCJzdWIiOiJhLXVzZXItaWQiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiU2VjdXJpdHlDaGVja0NyZWRlbnRpYWwiXSwiZXZpZGVuY2UiOlt7InR5cGUiOiJTZWN1cml0eUNoZWNrIiwibm90QUNvbnRyYUluZGljYXRvciI6W3siY29kZSI6IkQwMSIsImlzc3VhbmNlRGF0ZSI6IjIwMjItMDktMjBUMTU6NTQ6NTAuMDAwWiIsImRvY3VtZW50IjoicGFzc3BvcnQvR0JSLzgyNDE1OTEyMSIsInR4biI6WyJhYmNkZWYiXSwibWl0aWdhdGlvbiI6W3siY29kZSI6Ik0wMSIsIm1pdGlnYXRpbmdDcmVkZW50aWFsIjpbeyJpc3N1ZXIiOiJodHRwczovL2NyZWRlbnRpYWwtaXNzdWVyLmV4YW1wbGUvIiwidmFsaWRGcm9tIjoiMjAyMi0wOS0yMVQxNTo1NDo1MC4wMDBaIiwidHhuIjoiZ2hpaiIsImlkIjoidXJuOnV1aWQ6ZjgxZDRmYWUtN2RlYy0xMWQwLWE3NjUtMDBhMGM5MWU2YmY2In1dfV0sImluY29tcGxldGVNaXRpZ2F0aW9uIjpbeyJjb2RlIjoiTTAyIiwibWl0aWdhdGluZ0NyZWRlbnRpYWwiOlt7Imlzc3VlciI6Imh0dHBzOi8vYW5vdGhlci1jcmVkZW50aWFsLWlzc3Vlci5leGFtcGxlLyIsInZhbGlkRnJvbSI6IjIwMjItMDktMjJUMTU6NTQ6NTAuMDAwWiIsInR4biI6ImNkZWVmIiwiaWQiOiJ1cm46dXVpZDpmNWM5ZmY0MC0xZGNkLTRhOGItYmY5Mi05NDU2MDQ3YzEzMmYifV19XX1dfV19fQ._2dCakEuFyF861YIxn7XJvBs03vbmPfX3H51YuUyn53sFDKJPZZzgAN_qMIphEfTlUMxclKtCu0b_ycseW3bFQ"; // pragma: allowlist secret
const PASSPORT_VC =
  "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Jldmlldy1wLnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJzdWIiOiJ1cm46dXVpZDpiZmQzM2QyYi0yYTAzLTQ0MWMtOTBkYi00NGFlZDdhM2E3ZWYiLCJuYmYiOjE3MDg1MDY3OTMsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJJZGVudGl0eUNoZWNrQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU3ViamVjdCI6eyJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTk2NS0wNy0wOCJ9XSwicGFzc3BvcnQiOlt7ImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3IiwiaWNhb0lzc3VlckNvZGUiOiJHQlIiLCJleHBpcnlEYXRlIjoiMjAzMC0wMS0wMSJ9XSwibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLRU5ORVRISEhIIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiREVDRVJRVUVJUkEifV19XX0sImV2aWRlbmNlIjpbeyJ0eXBlIjoiSWRlbnRpdHlDaGVjayIsInR4biI6IjAzMmY4YjA3LWYxYWYtNDVhZC1hZjE4LWMyNjU2NTkxY2QyOCIsInN0cmVuZ3RoU2NvcmUiOjQsInZhbGlkaXR5U2NvcmUiOjAsImNpIjpbIkQwMiJdLCJmYWlsZWRDaGVja0RldGFpbHMiOlt7ImNoZWNrTWV0aG9kIjoiZGF0YSIsImRhdGFDaGVjayI6InJlY29yZF9jaGVjayJ9XSwiY2lSZWFzb25zIjpbeyJjaSI6IkQwMiIsInJlYXNvbiI6Ik5vTWF0Y2hpbmdSZWNvcmQifV19XX0sImp0aSI6InVybjp1dWlkOjM4ODBlYzg3LWIwZGMtNDYzNy04ZTRkLTA2NjU0YjFkNzY2ZiJ9.XeggA5BpYrxGxwyDKSZaZG4_7YiTR62m4tgLhncsNrT9V0NT_RBAYeIRY-dIuccnPLbn1rEYGt9XRwcP9NYVbg"; // pragma: allowlist secret
const DRIVING_PERMIT_VC =
  "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Jldmlldy1kLnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJzdWIiOiJ1cm46dXVpZDo0NTdkODgwMS0wN2VmLTRmNmQtOWI0Ny04MDY5YjI2YjFjOTYiLCJuYmYiOjE3MDg1MDgyNzcsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJJZGVudGl0eUNoZWNrQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU3ViamVjdCI6eyJkcml2aW5nUGVybWl0IjpbeyJwZXJzb25hbE51bWJlciI6IkRFQ0VSNjA3MDg1S0U5TE4iLCJleHBpcnlEYXRlIjoiMjA0Mi0xMC0wMSIsImlzc3VlTnVtYmVyIjoiMjMiLCJpc3N1ZWRCeSI6IkRWTEEiLCJpc3N1ZURhdGUiOiIyMDE4LTA0LTE5In1dLCJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InR5cGUiOiJHaXZlbk5hbWUiLCJ2YWx1ZSI6IktFTk5FVEgifSx7InR5cGUiOiJGYW1pbHlOYW1lIiwidmFsdWUiOiJERUNFUlFVRUlSQSJ9XX1dLCJhZGRyZXNzIjpbeyJpZCI6bnVsbCwicG9Cb3hOdW1iZXIiOm51bGwsInN1YkJ1aWxkaW5nTmFtZSI6bnVsbCwiYnVpbGRpbmdOdW1iZXIiOm51bGwsImJ1aWxkaW5nTmFtZSI6bnVsbCwic3RyZWV0TmFtZSI6bnVsbCwiYWRkcmVzc0xvY2FsaXR5IjpudWxsLCJwb3N0YWxDb2RlIjoiQkEyIDVBQSIsImFkZHJlc3NDb3VudHJ5IjoiR0IifV0sImJpcnRoRGF0ZSI6W3sidmFsdWUiOiIxOTY1LTA3LTA4In1dfSwiZXZpZGVuY2UiOlt7InR5cGUiOiJJZGVudGl0eUNoZWNrIiwidHhuIjoiNTNkZGU2ODEtYzZhZC00MzYzLWFkZWYtZmM0MDAxZmI2MTZiIiwiYWN0aXZpdHlIaXN0b3J5U2NvcmUiOjAsInN0cmVuZ3RoU2NvcmUiOjMsInZhbGlkaXR5U2NvcmUiOjAsImNpIjpbIkQwMiJdLCJmYWlsZWRDaGVja0RldGFpbHMiOlt7ImNoZWNrTWV0aG9kIjoiZGF0YSIsImlkZW50aXR5Q2hlY2tQb2xpY3kiOiJwdWJsaXNoZWQifV19XX0sImp0aSI6InVybjp1dWlkOjYxMDY1YTk2LTA4NDgtNDAxNy05YjE0LWFhODllOGI3ZDAzOCJ9.9M862Le368uXMxzUFpBKg13tBDCZErFXRcyjAuspthTho7qubpvuBkPNqiXL-rmi8ZzvRzY1o3St8iE8uvQV8A"; // pragma: allowlist secret

const mockPersist = jest
  .spyOn(cimitStubItemService, "persistCimitStubItem")
  .mockImplementation(async () => {});

beforeEach(() => {
  jest.clearAllMocks();
});

test("addUserCis should insert", async () => {
  const putContraIndicatorsRequest: PutContraIndicatorRequest = {
    govuk_signin_journey_id: "govuk_signin_journey_id",
    ip_address: "ip_address",
    signed_jwt: SIGNED_CRI_VC_WITH_ONE_CI,
  };

  const expectedCiRecordValues = {
    userId: "urn:uuid:c23ec166-2c32-4f00-bdfe-b29398e36801",
    contraIndicatorCode: "V03",
    issuer: "https://kbv-cri.stubs.account.gov.uk",
    issuanceDate: "2023-08-17T10:20:53Z",
    mitigations: [],
    document: "",
    sortKey: "V03#2023-08-17T10:20:53Z",
    txn: "18fbfe93-2e71-44bb-88c5-0f7de60fbe80",
    ttl: expect.any(Number),
  };

  await addUserCIs(putContraIndicatorsRequest);

  expect(mockPersist).toHaveBeenCalledTimes(1);
  expect(mockPersist).toHaveBeenCalledWith(
    expect.objectContaining(expectedCiRecordValues),
  );
});

test("addUserCisShouldFailIfNoEvidence", async () => {
  const putContraIndicatorsRequest: PutContraIndicatorRequest = {
    govuk_signin_journey_id: "govuk_signin_journey_id",
    ip_address: "ip_address",
    signed_jwt: SIGNED_CRI_VC_NO_EVIDENCE,
  };

  await addUserCIs(putContraIndicatorsRequest);

  expect(mockPersist).toHaveBeenCalledTimes(0);
});

test("addUserCisShouldFailIfJWTParsingFails", async () => {
  const putContraIndicatorsRequest: PutContraIndicatorRequest = {
    govuk_signin_journey_id: "govuk_signin_journey_id",
    ip_address: "ip_address",
    signed_jwt: "invalid_jwt",
  };

  await expect(addUserCIs(putContraIndicatorsRequest)).rejects.toThrowError(
    "Failed to add user CIs",
  );

  expect(mockPersist).toHaveBeenCalledTimes(0);
});

test("addUserCisShouldFailIfInvalidEvidence", async () => {
  const putContraIndicatorsRequest: PutContraIndicatorRequest = {
    govuk_signin_journey_id: "govuk_signin_journey_id",
    ip_address: "ip_address",
    signed_jwt: SIGNED_CRI_VC_INVALID_EVIDENCE,
  };

  await addUserCIs(putContraIndicatorsRequest);

  expect(mockPersist).toHaveBeenCalledTimes(0);
});

test("addUserCisShouldStorePassportIdentifierIfPresent", async () => {
  const putContraIndicatorsRequest: PutContraIndicatorRequest = {
    govuk_signin_journey_id: "govuk_signin_journey_id",
    ip_address: "ip_address",
    signed_jwt: PASSPORT_VC,
  };

  const expectedCiRecordValues = {
    document: "passport/GBR/321654987",
  };

  await addUserCIs(putContraIndicatorsRequest);

  expect(mockPersist).toHaveBeenCalledTimes(1);
  expect(mockPersist).toHaveBeenCalledWith(
    expect.objectContaining(expectedCiRecordValues),
  );
});

test("addUserCisShouldStoreDrivingPermitIdentifierIfPresent", async () => {
  const putContraIndicatorsRequest: PutContraIndicatorRequest = {
    govuk_signin_journey_id: "govuk_signin_journey_id",
    ip_address: "ip_address",
    signed_jwt: DRIVING_PERMIT_VC,
  };

  const expectedCiRecordValues = {
    document: "drivingPermit/GB/DVLA/DECER607085KE9LN/2018-04-19", // pragma: allowlist secret
  };

  await addUserCIs(putContraIndicatorsRequest);

  expect(mockPersist).toHaveBeenCalledTimes(1);
  expect(mockPersist).toHaveBeenCalledWith(
    expect.objectContaining(expectedCiRecordValues),
  );
});
