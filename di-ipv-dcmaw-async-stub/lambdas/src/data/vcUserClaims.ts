import { TestUser } from "../domain/managementEnqueueRequest";

export const USER_CLAIMS = {
  [TestUser.kennethD]: {
    name: [
      {
        nameParts: [
          {
            value: "Kenneth",
            type: "GivenName",
          },
          {
            value: "Decerqueira",
            type: "FamilyName",
          },
        ],
      },
    ],
    birthDate: [
      {
        value: "1965-07-08",
      },
    ],
  },
};
