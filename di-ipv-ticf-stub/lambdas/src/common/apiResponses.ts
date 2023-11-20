export const apiResponses = {
    _200: (body: { [key: string]: any }) => {
        return {
            statusCode: 200,
            body: JSON.stringify(body, null, 2),
        };
    },
    _400: (body?: { [key: string]: any }) => {
        if (!body) {[
            body = {errorMessage: "Bad request."}
        ]}
        return {
            statusCode: 400,
            body: JSON.stringify(body, null, 2),
        };
    },
    _500: (body?: { [key: string]: any }) => {
        if (!body) {[
            body = {errorMessage: "Error while retrieving TicF CRI VC."}
        ]}
        return {
            statusCode: 500,
            body: JSON.stringify(body, null, 2),
        };
    },
};

export default apiResponses;