import { post } from "../../../shared/api/client";
import { account } from "../../../shared/api/validation";
export interface RegisterInput {
  loginId: string;
  password: string;
  displayName: string;
}
export const register = (input: RegisterInput) =>
  post("/accounts", input, account);
