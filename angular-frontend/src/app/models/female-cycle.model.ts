export type CycleFlow = 'NONE' | 'LIGHT' | 'MEDIUM' | 'HEAVY';

export interface FemaleCycleEntry {
  id: string;
  date: string;
  periodStarted: boolean;
  flow: CycleFlow;
  mood: 'VERY_BAD' | 'BAD' | 'OK' | 'GOOD' | 'VERY_GOOD';
  notes?: string;
  createdAt: string;
}

export type CyclePhase =
  | 'MENSTRUATION'
  | 'FOLLICULAR'
  | 'OVULATION'
  | 'LUTEAL'
  | 'UNKNOWN';

export interface FemaleCycleStatus {
  cycleDay: number;
  phase: CyclePhase;
  lastPeriodStart: string;
  nextExpectedPeriod: string;
  daysToNextPeriod: number;
}
