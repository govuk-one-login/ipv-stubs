import { describe, it, expect } from "vitest";
import getErrorMessage from "../../src/common/errorReporting";

const TEST_MESSAGE = "Test message";

describe("getErrorMessage", () => {
  it("returns a message from an Error object", () => {
    // arrange
    const error = new Error(TEST_MESSAGE);

    // act
    const result = getErrorMessage(error);

    // assert
    expect(result).toEqual(TEST_MESSAGE);
  });

  it("serializes an object without a message property", () => {
    // arrange
    const error = {
      myMessage: TEST_MESSAGE,
    };

    // act
    const result = getErrorMessage(error);

    // assert
    expect(result).toEqual('{"myMessage":"Test message"}');
  });

  it("serializes an error that can't be converted to a JSON string", () => {
    // arrange
    const child = {};
    const error = {
      myMessage: TEST_MESSAGE,
      child: child,
    };
    // Create a circular reference so the JSON serializer fails
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (child as any).parent = error;

    // act
    const result = getErrorMessage(error);

    // assert
    expect(result).toEqual("Couldn't get error message from: [object Object]");
  });
});
