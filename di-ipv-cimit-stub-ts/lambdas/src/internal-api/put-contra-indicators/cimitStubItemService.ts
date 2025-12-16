import { CimitStubItem } from "../../common/contraIndicatorTypes"
import { dynamoDBClient} from "../../clients/dynamoDBClient"
import { getCimitStubTableName } from "../../common/configService";
import { marshall } from "@aws-sdk/util-dynamodb";


const tableName = getCimitStubTableName();

export const persistCimitStubItem = (cimitStubItem: CimitStubItem): void => {
    // TODO set ttl with the cimitStubItem before it gets written
    dynamoDBClient.putItem({
        TableName: tableName,
        Item: marshall(cimitStubItem, {
            removeUndefinedValues: true,
        })
    });
}