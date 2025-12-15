import { PutContraIndicatorRequest } from "../internal-api/put-contra-indicators/putContraIndicatorsHandler";
import { getErrorMessage } from "./apiResponseBuilder";


export const addUserCIs = (request: PutContraIndicatorRequest): void => {
    try {
        console.log("making database call");
    } catch (error) {
        console.error(getErrorMessage(error));
        throw new Error("Failed to add user CIs");
    }
};