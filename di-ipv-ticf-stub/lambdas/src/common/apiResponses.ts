export const apiResponses = {
  _200: (body: { [key: string]: any }) => ({
    statusCode: 200,
    body: JSON.stringify(body, undefined, 2),
  }),
  _400: (body?: { [key: string]: any }) => ({
    statusCode: 400,
    body: JSON.stringify(body ?? { errorMessage: "Bad request" }, undefined, 2),
  }),
  _500: (body?: { [key: string]: any }) => ({
    statusCode: 500,
    body: JSON.stringify(
      body ?? { errorMessage: "Error while retrieving TicF CRI VC" },
      undefined,
      2
    ),
  }),
};

export default apiResponses;
