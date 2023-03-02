const AWS = require('aws-sdk');
AWS.config.update({region: 'eu-west-2'});

export const handler = async(event) => {
    let response = {
        "isAuthorized": false,
        "context": {
            "Authorization": "failed"
        }
    };

    const auth = await getParam();
    var authorizationHeader = event.headers.Authorization
    var encodedCreds = authorizationHeader.split(' ')[1]
    var plainCreds = (new Buffer(encodedCreds, 'base64')).toString().split(':')
    var username = plainCreds[0]
    var password = plainCreds[1]

    if (username === auth.username && password === auth.password) {
        response = {
            "isAuthorized": true,
            "context": {
                "Authorization": "Succeeded"
            }
        };
    }
    return response;
};

async function getParam {
    const ssm = new AWS.SSM({region: 'eu-west-2'});

    const paramDetails = await ssm.getParameter({
      Name: '/stubs/core/cri/env/CORE_STUB_API_AUTH', /* required */
      WithDecryption: false
    }).promise();
    const data = JSON.parse(paramDetails.Parameter.Value);
    return data;
};
