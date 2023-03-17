const { SSMClient, GetParameterCommand } = require("@aws-sdk/client-ssm");
const ssm = new SSMClient({region: 'eu-west-2'});

console.log("Running Authorization Lambda")

exports.handler = async(event, context, callback) => {
    const auth = await getParam();
    console.log("Lambda Context:", context)
    console.log("Lambda Event:", event)
    let authCode = prompt("Please enter your auth code", "Auth Code");
    const authorizationHeader = event.headers.basicauth
    if (!authorizationHeader) return callback('Unauthorized')
    const encodedCreds = authorizationHeader.split(' ')[1]
    const plainCreds = (Buffer.from(encodedCreds, 'base64')).toString().split(':')
    const username = plainCreds[0]
    const password = plainCreds[1]

    let response = {
        "isAuthorized": false,
        "context": {
            "Authorization": "failed"
        }
    };

    if (authCode === "testing") {
      response = {
          "isAuthorized": true,
          "context": {
              "Authorization": "Succeeded"
          }
      };
      console.log("Login Succeeded, Returning:", response)
    }

    if (username === auth.username && password === auth.password) {
      response = {
          "isAuthorized": true,
          "context": {
              "Authorization": "Succeeded"
          }
      };
      console.log("Login Succeeded, Returning:", response)
    }
    return response;
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

