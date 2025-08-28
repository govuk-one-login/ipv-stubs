export class InvalidAccessToken extends Error {
  name: string;

  constructor(message: string) {
    super();
    this.message = message;
    this.name = "Invalid Access Token";
  }
}

export class InvalidAuthHeader extends Error {
  name: string;

  constructor(message: string) {
    super();
    this.message = message;
    this.name = "Invalid Auth Header";
  }
}
