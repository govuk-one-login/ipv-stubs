import { JWTPayload, importSPKI, jwtVerify } from "jose";

const EC_PUBLIC_KEY =
  "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEE9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIMqVMxm2EdlSRjPkCV5NDyN9/RMmJLerY4H0vkXDjEDTg==";

export const verifyToken = async(jwt: string): Promise<boolean> => {
    let payload;
    try {
        const key = await importSPKI(
            `-----BEGIN PUBLIC KEY-----\n${EC_PUBLIC_KEY}\n-----END PUBLIC KEY-----`,
            "ES256"
        );
        payload = (await jwtVerify(jwt, key)).payload as JWTPayload;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (error: any) {
        throw Error(error);
    }
    return (payload !== null);
}
