import cases from "../cases";

export interface Response {
  userId: string;
  statusCode: number;
  ttl: number;
  responseDelay: number;
  responseBody: {
    intervention: Intervention;
    state: State;
    auditLevel: string;
    history: History[];
  };
}

interface Intervention {
  updatedAt: number;
  appliedAt: number;
  sentAt: number;
  description: string;
  reprovedIdentityAt: number;
  resetPasswordAt: number;
  accountDeletedAt: number;
}

interface State {
  blocked: boolean;
  suspended: boolean;
  reproveIdentity: boolean;
  resetPassword: boolean;
}

interface History {
  sentAt: number;
  component: string;
  code: string;
  intervention: string;
  reason: string;
  originatingComponent: string;
  originatorReferenceId: string;
  requesterId: string;
}

export type UserManagementRequest = Pick<
  Response,
  "statusCode" | "responseDelay"
> & {
  intervention: keyof typeof cases;
};
