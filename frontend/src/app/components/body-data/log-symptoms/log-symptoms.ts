import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CycleEntryService, CycleEntry } from '../../../services/cycle-entry.service';

export type FlowIntensity = 'LIGHT' | 'MEDIUM' | 'HEAVY' | 'SPOTTING' | null;
export type SleepQuality = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR' | null;
export type Mood = 'HAPPY' | 'IRRITABLE' | 'ANXIOUS' | 'LOW_ENERGY' | 'FOCUSED' | 'CALM' | null;

interface PhysicalSymptom {
  id: string;
  label: string;
}

interface MoodOption {
  id: Mood;
  label: string;
  icon: string;
}

interface FlowOption {
  id: FlowIntensity;
  label: string;
}

@Component({
  selector: 'app-log-symptoms',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './log-symptoms.html',
  styleUrl: './log-symptoms.scss'
})
export class LogSymptoms implements OnInit {
  @ViewChild('symptomDialog') symptomDialog!: ElementRef<HTMLDialogElement>;

  today = new Date();

  physicalOptions: PhysicalSymptom[] = [
    { id: 'BLOATING', label: 'Bloating' },
    { id: 'CRAMPS', label: 'Cramps' },
    { id: 'BREAST_TENDERNESS', label: 'Breast Tenderness' },
    { id: 'HEADACHE', label: 'Headache' },
    { id: 'ACNE', label: 'Acne' },
    { id: 'MUSCLE_SORENESS', label: 'Muscle Soreness' },
  ];

  moodOptions: MoodOption[] = [
    { id: 'HAPPY', label: 'Happy', icon: 'sentiment_very_satisfied' },
    { id: 'IRRITABLE', label: 'Irritable', icon: 'sentiment_dissatisfied' },
    { id: 'ANXIOUS', label: 'Anxious', icon: 'psychology_alt' },
    { id: 'LOW_ENERGY', label: 'Low Energy', icon: 'battery_low' },
    { id: 'FOCUSED', label: 'Focused', icon: 'center_focus_strong' },
    { id: 'CALM', label: 'Calm', icon: 'self_improvement' },
  ];

  flowOptions: FlowOption[] = [
    { id: 'LIGHT', label: 'Light' },
    { id: 'MEDIUM', label: 'Medium' },
    { id: 'HEAVY', label: 'Heavy' },
    { id: 'SPOTTING', label: 'Spotting' },
  ];

  selectedSymptoms = new Set<string>();
  selectedMood: Mood = null;
  energyLevel = 5;
  sleepHours = 8;
  sleepMinutes = 0;
  sleepQuality: SleepQuality = null;
  flowIntensity: FlowIntensity = null;
  notes = '';

  saving = false;
  error: string | null = null;
  savedEntry: Partial<CycleEntry> | null = null;

  constructor(
    private router: Router,
    private cycleEntryService: CycleEntryService,
  ) {}

  ngOnInit(): void {
    // Pre-load today's entry if it exists
    this.cycleEntryService.getByDate(this.today).subscribe({
      next: entry => this.populateFromEntry(entry),
      error: () => { /* no entry for today yet */ }
    });
  }

  private populateFromEntry(entry: CycleEntry): void {
    if (entry.physicalSymptoms) {
      entry.physicalSymptoms.split(',').forEach(s => this.selectedSymptoms.add(s.trim()));
    }
    this.selectedMood = (entry.mood as Mood) ?? null;
    this.energyLevel = entry.energyLevel ?? 5;
    this.sleepHours = entry.sleepHours ?? 8;
    this.sleepMinutes = entry.sleepMinutes ?? 0;
    this.sleepQuality = (entry.sleepQuality as SleepQuality) ?? null;
    this.flowIntensity = (entry.flowIntensity as FlowIntensity) ?? null;
    this.notes = entry.notes ?? '';
  }

  toggleSymptom(id: string): void {
    if (this.selectedSymptoms.has(id)) {
      this.selectedSymptoms.delete(id);
    } else {
      this.selectedSymptoms.add(id);
    }
  }

  isSymptomSelected(id: string): boolean {
    return this.selectedSymptoms.has(id);
  }

  selectMood(mood: Mood): void {
    this.selectedMood = this.selectedMood === mood ? null : mood;
  }

  selectFlow(flow: FlowIntensity): void {
    this.flowIntensity = this.flowIntensity === flow ? null : flow;
  }

  get energyLabel(): string {
    return String(this.energyLevel).padStart(2, '0');
  }

  get sleepLabel(): string {
    return `${this.sleepHours}h ${this.sleepMinutes.toString().padStart(2, '0')}m`;
  }

  save(): void {
    this.saving = true;
    this.error = null;

    const entry: Partial<CycleEntry> = {
      entryDate: this.today.toISOString().split('T')[0],
      physicalSymptoms: Array.from(this.selectedSymptoms).join(','),
      mood: this.selectedMood ?? undefined,
      energyLevel: this.energyLevel,
      sleepHours: this.sleepHours,
      sleepMinutes: this.sleepMinutes,
      sleepQuality: this.sleepQuality ?? undefined,
      flowIntensity: this.flowIntensity ?? undefined,
      notes: this.notes || undefined,
    };

    this.cycleEntryService.create(entry as CycleEntry).subscribe({
      next: () => {
        this.saving = false;
        this.savedEntry = entry;
        this.symptomDialog.nativeElement.showModal();
      },
      error: () => {
        this.saving = false;
        this.error = 'Failed to save. Please try again.';
      }
    });
  }

  discard(): void {
    this.router.navigate(['/body-data/cycle-tracking']);
  }

  closeDialog(): void {
    this.symptomDialog.nativeElement.close();
    this.router.navigate(['/body-data/cycle-tracking']);
  }

  onDialogBackdropClick(event: MouseEvent): void {
    const rect = this.symptomDialog.nativeElement.getBoundingClientRect();
    const isOutside = event.clientX < rect.left || event.clientX > rect.right
      || event.clientY < rect.top || event.clientY > rect.bottom;
    if (isOutside) this.closeDialog();
  }

  savedSymptomsList(): string[] {
    return (this.savedEntry?.physicalSymptoms ?? '')
      .split(',')
      .map(s => s.trim())
      .filter(Boolean);
  }

  savedSleepLabel(): string {
    const h = this.savedEntry?.sleepHours ?? 0;
    const m = this.savedEntry?.sleepMinutes ?? 0;
    return `${h}h ${m.toString().padStart(2, '0')}m`;
  }

  moodLabel(id: string | undefined): string {
    return this.moodOptions.find(m => m.id === id)?.label ?? '';
  }

  moodIcon(id: string | undefined): string {
    return this.moodOptions.find(m => m.id === id)?.icon ?? '';
  }

  symptomLabel(id: string): string {
    return this.physicalOptions.find(p => p.id === id)?.label ?? id;
  }

  flowLabel(id: string | undefined): string {
    return this.flowOptions.find(f => f.id === id)?.label ?? '';
  }
}
