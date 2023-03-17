const { SSMClient, GetParameterCommand } = require("@aws-sdk/client-ssm");
const ssm = new SSMClient({region: 'eu-west-2'});

console.log("Running Authorization Lambda")

exports.handler = async(event, context, callback) => {
    const auth = await getParam();
    console.log("Lambda Context:", context)
    console.log("Lambda Event:", event)
    const authorizationHeader = event.headers.authorization
    if (!authorizationHeader) return callback('Unauthorized')
    const encodedCreds = authorizationHeader.split(' ')[1]
    const plainCreds = (Buffer.from(encodedCreds, 'base64')).toString().split(':')
    const username = plainCreds[0]
    const password = plainCreds[1]

    /*
      console.log(auth)
      console.log(encodedCreds)
      console.log(plainCreds)
    */

    if (!(username === auth.username && password === auth.password)) return callback('Unauthorized')

    const authResponse = buildAllowAllPolicy(event, username)
    console.log("Login Succeeded, Returning:", authResponse)
    callback(null, authResponse)
}

async function getParam() {
    const command = new GetParameterCommand({
      Name: '/stubs/core/cri/env/CORE_STUB_API_AUTH',
      WithDecryption: false
    });
    const paramDetails = await ssm.send(command);
    //console.log(paramDetails)
    const data = JSON.parse(paramDetails.Parameter.Value);
    return data;
}

function buildAllowAllPolicy (event, principalId) {
  var apiOptions = {}
  var tmp = event.methodArn.split(':')
  var apiGatewayArnTmp = tmp[5].split('/')
  var awsAccountId = tmp[4]
  var awsRegion = tmp[3]
  var restApiId = apiGatewayArnTmp[0]
  var stage = apiGatewayArnTmp[1]
  var apiArn = 'arn:aws:execute-api:' + awsRegion + ':' + awsAccountId + ':' +
    restApiId + '/' + stage + '/*/*'
  const policy = {
    principalId: principalId,
    policyDocument: {
      Version: '2012-10-17',
      Statement: [
        {
          Action: 'execute-api:Invoke',
          Effect: 'Allow',
          Resource: [apiArn]
        }
      ]
    }
  }
  return policy
};
