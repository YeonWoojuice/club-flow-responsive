// 백엔드 도메인 명칭(ClubStaffRole, club_staffs.role)과 일치시킨다.
export type ClubStaffRole = "PRESIDENT" | "VICE_PRESIDENT" | "STAFF";
export type ClubStaffStatus = "PENDING" | "APPROVED" | "REJECTED" | "REVOKED";

// 백엔드 ClubResponse에 대응: Club 정보 + 현재 사용자의 운영진 역할·상태 합성.
export type Club = {
  id: string;
  name: string;
  description: string | null;
  role: ClubStaffRole;
  status: ClubStaffStatus;
  createdAt: string;
};

export type CreateClubInput = {
  name: string;
  description: string;
};
