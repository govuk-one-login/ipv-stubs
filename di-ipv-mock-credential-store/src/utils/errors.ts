export class CustomError extends Error {
  statusCode: number;
  override message: string;
  object?: object | undefined;

  constructor(statusCode: number, message: string, object?: object | undefined) {
    super();
    this.statusCode = statusCode;
    this.message = message;
    this.object = object;
  }
}
