module.exports = {
  tables: [
    {
      TableName: `evcs-stored-identity-store`,
      KeySchema: [
        { AttributeName: "userId", KeyType: "HASH" },
        { AttributeName: "recordType", KeyType: "RANGE" },
      ],
      AttributeDefinitions: [
        { AttributeName: "userId", AttributeType: "S" },
        { AttributeName: "recordType", AttributeType: "S" },
      ],
      ProvisionedThroughput: { ReadCapacityUnits: 1, WriteCapacityUnits: 1 },
    },
  ],
};
