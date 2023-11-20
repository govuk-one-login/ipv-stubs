export default interface TicfRequest {
  vtr: string;
  vot: string;
  vtm: string;
  sub: string;
  govuk_signin_journey_id: string;
  "https://vocab.account.gov.uk/v1/credentialJWT": string[];
}
