import type { components } from "./api.gen";

type Schemas = components["schemas"];
type MemberSchema = Schemas["GenerationMemberResponse"];
type HistorySchema = Schemas["GenerationMemberStatusHistoryResponse"];

export type MemberJoinedSource = NonNullable<MemberSchema["joinedSource"]>;
export type GenerationMemberStatus = NonNullable<MemberSchema["status"]>;

export type GenerationMember = Required<Omit<MemberSchema, "phone">> & {
  phone: string | null;
};

export type GenerationMemberStatusChangeRequest =
  Schemas["ChangeGenerationMemberStatusRequest"];

export type GenerationMemberStatusHistory = Required<Omit<HistorySchema, "reason">> & {
  reason: string | null;
};
