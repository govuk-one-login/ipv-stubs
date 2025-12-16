import { PutContraIndicatorRequest } from "../internal-api/put-contra-indicators/putContraIndicatorsHandler";
import { getErrorMessage } from "./apiResponseBuilder";
import { CimitStubItem } from "./contraIndicatorTypes";


export const addUserCIs = (request: PutContraIndicatorRequest): void => {
    try {
        console.log("in addUserCIs");
        const signedJwt = formatForDatabase(request);
        console.log("making database call");
    } catch (error) {
        console.error(getErrorMessage(error));
        throw new Error("Failed to add user CIs");
    }
};

// TODO find a way to make this method not exported
// should eventually return a CimitStubItem
export const formatForDatabase = (request: PutContraIndicatorRequest): void => {
    const signedJwt = JSON.parse(request.signed_jwt);
    console.log(signedJwt);
    return;
}