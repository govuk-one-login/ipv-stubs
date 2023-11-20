export default interface TicfResponse {
    sub: string,
    govuk_signin_journey_id: string,
    vtr: string,
    vot: string,
    vtm: string,
    "https://vocab.account.gov.uk/v1/credentialJWT": string[]
};