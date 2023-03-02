const AWS = require('aws-sdk');
AWS.config.update({region: 'eu-west-2'});
const ssm = new AWS.SSM({region: 'eu-west-2'});

export const handler = async(event) => {
    let response = {
        "isAuthorized": false,
        "context": {
            "Authorization": "failed"
        }
    };

    const auth = await getParam();
    const authorizationHeader = event.headers.Authorization
    const encodedCreds = authorizationHeader.split(' ')[1]
    const plainCreds = (new Buffer(encodedCreds, 'base64')).toString().split(':')
    const username = plainCreds[0]
    const password = plainCreds[1]

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
    const paramDetails = await ssm.getParameter({
      Name: '/stubs/core/cri/env/CORE_STUB_API_AUTH', /* required */
      WithDecryption: false
    }).promise();
    const data = JSON.parse(paramDetails.Parameter.Value);
    return data;
};


