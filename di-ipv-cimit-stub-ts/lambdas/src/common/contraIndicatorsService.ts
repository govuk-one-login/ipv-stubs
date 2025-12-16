import { PutContraIndicatorRequest } from "../internal-api/put-contra-indicators/putContraIndicatorsHandler";
import { getErrorMessage } from "./apiResponseBuilder";
import { CimitStubItem } from "./contraIndicatorTypes";


export const addUserCIs = (request: PutContraIndicatorRequest): void => {
    try {
        const signedJwt = formatForDatabase(request);
        console.log("making database call");
    } catch (error) {
        console.error(getErrorMessage(error));
        throw new Error("Failed to add user CIs");
    }
};

const formatForDatabase = (request: PutContraIndicatorRequest): CimitStubItem => {
    const signedJwt = JSON.parse(request.signed_jwt);
    console.log(signedJwt);
}