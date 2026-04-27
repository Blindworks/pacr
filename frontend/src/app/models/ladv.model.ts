export type LadvStagedEventStatus = 'NEW' | 'IMPORTED' | 'IGNORED';

export interface LadvImportSource {
  id: number;
  name: string;
  lv: string;
  bestenlistenfaehigOnly: boolean;
  enabled: boolean;
  lastFetchedAt: string | null;
  lastFetchStatus: string | null;
  createdAt: string;
}

export interface LadvCreateSourceRequest {
  name: string;
  lv: string;
  bestenlistenfaehigOnly?: boolean;
  enabled?: boolean;
}

export interface LadvDistance {
  name: string | null;
  meters: number | null;
}

export interface LadvStagedEvent {
  id: number;
  sourceId: number;
  sourceName: string;
  ladvId: number;
  veranstaltungsnummer: string | null;
  name: string;
  startDate: string;
  endDate: string | null;
  startTime: string | null;
  kategorie: string | null;
  veranstalter: string | null;
  homepage: string | null;
  ort: string | null;
  plz: string | null;
  latitude: number | null;
  longitude: number | null;
  abgesagt: boolean;
  distances: LadvDistance[];
  status: LadvStagedEventStatus;
  importedCompetitionId: number | null;
  importedAt: string | null;
  fetchedAt: string;
}

export interface LadvImportRunSummary {
  sourceId: number;
  sourceName: string;
  fetched: number;
  newItems: number;
  skipped: number;
  success: boolean;
  message: string;
}

export interface LadvLvOption {
  code: string;
  name: string;
}

export interface LadvStagedEventsPage {
  content: LadvStagedEvent[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
