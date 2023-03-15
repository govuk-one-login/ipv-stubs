const { SSMClient } = require("@aws-sdk/client-ssm");
const ssm = new SSMClient({region: 'eu-west-2'});

/*
exports.handler = function (event, context, callback) {
  var authorizationHeader = event.headers.Authorization

  if (!authorizationHeader) return callback('Unauthorized')

  var encodedCreds = authorizationHeader.split(' ')[1]
  var plainCreds = (new Buffer(encodedCreds, 'base64')).toString().split(':')
  var username = plainCreds[0]
  var password = plainCreds[1]
  const auth = await getParam();

  if (!(username === auth.username && password === auth.password)) return callback('Unauthorized')

  var authResponse = buildAllowAllPolicy(event, username)

  callback(null, authResponse)
}
*/

console.log("Running Script")

exports.handler = async(event, context, callback) => {
    const auth = await getParam();
    const authorizationHeader = event.headers.Authorization
    const encodedCreds = authorizationHeader.split(' ')[1]
    const plainCreds = (new Buffer(encodedCreds, 'base64')).toString().split(':')
    const username = plainCreds[0]
    const password = plainCreds[1]
    console.log(auth)

    if (!authorizationHeader) return callback('Unauthorized')

    console.log(auth)
    console.log(encodedCreds)
    console.log(plainCreds)

    if (!(username === auth.username && password === auth.password)) return callback('Unauthorized')
    callback(null, authResponse)
}

async function getParam() {
    const paramDetails = await ssm.getParameter({
      Name: '/stubs/core/cri/env/CORE_STUB_API_AUTH', /* required */
      WithDecryption: false
    });
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
