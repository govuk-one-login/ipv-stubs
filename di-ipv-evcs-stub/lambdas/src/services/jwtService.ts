import { JWTPayload, importSPKI, jwtVerify } from "jose";
import { config } from "../common/config";
import { getSsmParameter } from "../common/ssmParameter";

export const verifyToken = async(jwt: string): Promise<boolean> => {
    const EVCS_VERIFY_KEY = await getSsmParameter(
        config.evcsParamBasePath + "verifyKey"
    );

    let payload;
    try {
        const key = await importSPKI(
            `-----BEGIN PUBLIC KEY-----\n${EVCS_VERIFY_KEY}\n-----END PUBLIC KEY-----`,
            "ES256"
        );
        payload = (await jwtVerify(jwt, key)).payload as JWTPayload;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (error: any) {
        throw Error(error);
    }
    return (payload !== null);
}
