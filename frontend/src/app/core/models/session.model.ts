export interface SessionSummary {
  id: string;
  name: string | null;
  comment: string | null;
  ownerDiscordId: string | null;
  startTime: string | null;
  endTime: string | null;
  sessionDuration: string | null;
  loot: number;
  supplies: number;
  balance: number;
  processDate: string | null;
}

export interface SessionMember {
  id: string;
  name: string;
  loot: number;
  supplies: number;
  balance: number;
  damage: number;
  healing: number;
}

export interface SessionTransfer {
  id: string;
  fromPlayer: string;
  toPlayer: string;
  amount: number;
}

export interface SessionDetail extends SessionSummary {
  members: SessionMember[];
  transfers: SessionTransfer[];
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SessionListFilter {
  name?: string;
  player?: string;
  startDate?: string;
  endDate?: string;
  ownerDiscordId?: string;
  page?: number;
  size?: number;
  sort?: string;
}
