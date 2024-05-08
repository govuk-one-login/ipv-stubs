module.exports = {
  tables: [
    {
      TableName: `evcs-stub-user-vcs-store`,
      KeySchema: [{ AttributeName: "userId", KeyType: "HASH"}, {AttributeName: "vcSignature", KeyType: "RANGE"}],
      AttributeDefinitions: [{ AttributeName: "userId", AttributeType: "S" }, {AttributeName: "vcSignature", AttributeType: "S"}],
      ProvisionedThroughput: { ReadCapacityUnits: 1, WriteCapacityUnits: 1 },
    },
  ],
};
