export interface ApiResponse {
  messageId: string;
  statusCode?: number;
}

export interface ErrorResponse {
  message: string;
  statusCode?: number;
}

export default ApiResponse;