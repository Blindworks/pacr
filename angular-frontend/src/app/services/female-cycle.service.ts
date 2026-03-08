import { Injectable } from '@angular/core';

import { CycleFlow, CyclePhase, FemaleCycleEntry, FemaleCycleStatus } from '../models/female-cycle.model';

@Injectable({
  providedIn: 'root'
})
export class FemaleCycleService {
  private readonly storageKey = 'pacr.female-cycle.entries.v1';

  getEntries(userId: number): FemaleCycleEntry[] {
    const all = this.loadAll();
    const entries = all[this.userKey(userId)] ?? [];
    return [...entries].sort((a, b) => b.date.localeCompare(a.date));
  }

  saveEntry(userId: number, entry: Omit<FemaleCycleEntry, 'id' | 'createdAt'>, existingId?: string): FemaleCycleEntry[] {
    const all = this.loadAll();
    const key = this.userKey(userId);
    const current = all[key] ?? [];

    const updatedEntry: FemaleCycleEntry = {
      id: existingId ?? this.createId(),
      createdAt: existingId
        ? current.find(e => e.id === existingId)?.createdAt ?? new Date().toISOString()
        : new Date().toISOString(),
      ...entry
    };

    const withoutOld = existingId ? current.filter(e => e.id !== existingId) : current;
    const updated = [...withoutOld, updatedEntry].sort((a, b) => b.date.localeCompare(a.date));

    all[key] = updated;
    this.saveAll(all);
    return updated;
  }

  deleteEntry(userId: number, entryId: string): FemaleCycleEntry[] {
    const all = this.loadAll();
    const key = this.userKey(userId);
    const current = all[key] ?? [];
    const updated = current.filter(entry => entry.id !== entryId);

    all[key] = updated;
    this.saveAll(all);
    return [...updated].sort((a, b) => b.date.localeCompare(a.date));
  }

  getCurrentStatus(entries: FemaleCycleEntry[], cycleLength = 28, periodLength = 5): FemaleCycleStatus | null {
    const normalizedCycleLength = Math.max(21, Math.min(40, cycleLength));
    const normalizedPeriodLength = Math.max(2, Math.min(10, periodLength));

    const sortedAsc = [...entries].sort((a, b) => a.date.localeCompare(b.date));
    const lastPeriodStart = [...sortedAsc].reverse().find(entry => entry.periodStarted);

    if (!lastPeriodStart) {
      return null;
    }

    const startDate = this.parseIsoDate(lastPeriodStart.date);
    const today = this.startOfDay(new Date());

    const daysSinceStart = Math.floor((today.getTime() - startDate.getTime()) / (24 * 60 * 60 * 1000));
    const cycleDay = ((Math.max(daysSinceStart, 0)) % normalizedCycleLength) + 1;

    const phase = this.resolvePhase(cycleDay, normalizedCycleLength, normalizedPeriodLength);
    const nextExpected = new Date(startDate);
    nextExpected.setDate(nextExpected.getDate() + normalizedCycleLength);

    const daysToNextPeriod = Math.ceil((nextExpected.getTime() - today.getTime()) / (24 * 60 * 60 * 1000));

    return {
      cycleDay,
      phase,
      lastPeriodStart: this.toIsoDate(startDate),
      nextExpectedPeriod: this.toIsoDate(nextExpected),
      daysToNextPeriod
    };
  }

  getFlowLabel(flow: CycleFlow): string {
    switch (flow) {
      case 'LIGHT':
        return 'Leicht';
      case 'MEDIUM':
        return 'Mittel';
      case 'HEAVY':
        return 'Stark';
      default:
        return 'Keine';
    }
  }

  private resolvePhase(cycleDay: number, cycleLength: number, periodLength: number): CyclePhase {
    if (cycleDay <= periodLength) {
      return 'MENSTRUATION';
    }

    const ovulationDay = Math.max(12, cycleLength - 14);
    const ovulationWindowEnd = Math.min(cycleLength, ovulationDay + 1);

    if (cycleDay < ovulationDay) {
      return 'FOLLICULAR';
    }

    if (cycleDay <= ovulationWindowEnd) {
      return 'OVULATION';
    }

    if (cycleDay <= cycleLength) {
      return 'LUTEAL';
    }

    return 'UNKNOWN';
  }

  private loadAll(): Record<string, FemaleCycleEntry[]> {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return {};
    }

    try {
      return JSON.parse(raw) as Record<string, FemaleCycleEntry[]>;
    } catch {
      return {};
    }
  }

  private saveAll(data: Record<string, FemaleCycleEntry[]>): void {
    localStorage.setItem(this.storageKey, JSON.stringify(data));
  }

  private userKey(userId: number): string {
    return `user-${userId}`;
  }

  private createId(): string {
    return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }

  private parseIsoDate(isoDate: string): Date {
    const [year, month, day] = isoDate.split('-').map(Number);
    return new Date(year, month - 1, day);
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private startOfDay(date: Date): Date {
    const normalized = new Date(date);
    normalized.setHours(0, 0, 0, 0);
    return normalized;
  }
}
