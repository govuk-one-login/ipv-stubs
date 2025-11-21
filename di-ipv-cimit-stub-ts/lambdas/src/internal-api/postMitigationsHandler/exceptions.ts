export class FailedToParseRequestError extends Error {
  statusCode: number;
  name: string;

  constructor(message: string) {
    super();
    this.message = message;
    this.statusCode = 400;
    this.name = "Failed to Parse Request";
  }
}