import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  inject,
} from '@angular/core';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AddEntryRequest,
  PersonalRecord,
  PersonalRecordEntry,
  PersonalRecordService,
} from '../../services/personal-record.service';

type TabId = 'history' | 'edit';

@Component({
  selector: 'app-personal-record-detail-dialog',
  standalone: true,
  imports: [NgIf, NgFor, FormsModule, DatePipe],
  templateUrl: './personal-record-detail-dialog.component.html',
  styleUrl: './personal-record-detail-dialog.component.scss',
})
export class PersonalRecordDetailDialogComponent implements OnInit {
  @ViewChild('dialog') private dialogEl!: ElementRef<HTMLDialogElement>;

  @Input() record!: PersonalRecord;

  /** Emitted when the dialog closes (record data may have changed). */
  @Output() closed = new EventEmitter<boolean>();

  private readonly service = inject(PersonalRecordService);

  // ── State ─────────────────────────────────────────────────────────────
  activeTab: TabId = 'history';

  entries: PersonalRecordEntry[] = [];
  loadingEntries = false;
  entriesError: string | null = null;

  deletingEntryId: number | null = null;

  goalTimeInput = '';
  savingGoal = false;
  goalError: string | null = null;
  goalSuccess = false;

  manualDateInput = '';
  manualTimeInput = '';
  addingEntry = false;
  addEntryError: string | null = null;

  /** Set to true when the caller should refresh its list after close. */
  private _dirty = false;

  // ── Lifecycle ─────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadEntries();
  }

  // ── Public API ────────────────────────────────────────────────────────
  open(): void {
    this.reset();
    this.loadEntries();
    this.dialogEl.nativeElement.showModal();
  }

  close(): void {
    this.dialogEl.nativeElement.close();
    this.closed.emit(this._dirty);
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogEl.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left ||
      event.clientX > rect.right ||
      event.clientY < rect.top ||
      event.clientY > rect.bottom;
    if (outside) this.close();
  }

  selectTab(tab: TabId): void {
    this.activeTab = tab;
  }

  // ── History tab ───────────────────────────────────────────────────────
  loadEntries(): void {
    if (!this.record) return;
    this.loadingEntries = true;
    this.entriesError = null;

    this.service.getPersonalRecordEntries(this.record.id).subscribe({
      next: (entries) => {
        this.entries = entries.slice().sort(
          (a, b) =>
            new Date(b.achievedDate).getTime() -
            new Date(a.achievedDate).getTime(),
        );
        this.loadingEntries = false;
      },
      error: () => {
        this.entriesError = 'Einträge konnten nicht geladen werden.';
        this.loadingEntries = false;
      },
    });
  }

  deleteEntry(entry: PersonalRecordEntry): void {
    this.deletingEntryId = entry.id;

    this.service
      .deletePersonalRecordEntry(this.record.id, entry.id)
      .subscribe({
        next: () => {
          this._dirty = true;
          this.deletingEntryId = null;
          this.loadEntries();
        },
        error: () => {
          this.deletingEntryId = null;
        },
      });
  }

  // ── Edit tab — goal time ───────────────────────────────────────────────
  saveGoal(): void {
    const seconds = this.parseTime(this.goalTimeInput);
    if (seconds === null) {
      this.goalError = 'Ungültiges Format. Bitte MM:SS oder H:MM:SS eingeben.';
      return;
    }

    this.savingGoal = true;
    this.goalError = null;
    this.goalSuccess = false;

    this.service.updatePersonalRecordGoal(this.record.id, seconds).subscribe({
      next: (updated) => {
        this.record = { ...this.record, goalTimeSeconds: updated.goalTimeSeconds };
        this.savingGoal = false;
        this.goalSuccess = true;
        this._dirty = true;
        setTimeout(() => (this.goalSuccess = false), 3000);
      },
      error: () => {
        this.savingGoal = false;
        this.goalError = 'Speichern fehlgeschlagen. Bitte erneut versuchen.';
      },
    });
  }

  // ── Edit tab — add manual entry ────────────────────────────────────────
  get canAddEntry(): boolean {
    return (
      !!this.manualDateInput &&
      this.parseTime(this.manualTimeInput) !== null
    );
  }

  addEntry(): void {
    if (!this.canAddEntry) return;

    const timeSeconds = this.parseTime(this.manualTimeInput)!;
    const request: AddEntryRequest = {
      timeSeconds,
      achievedDate: this.manualDateInput,
    };

    this.addingEntry = true;
    this.addEntryError = null;

    this.service.addPersonalRecordEntry(this.record.id, request).subscribe({
      next: () => {
        this._dirty = true;
        this.addingEntry = false;
        this.manualDateInput = '';
        this.manualTimeInput = '';
        this.activeTab = 'history';
        this.loadEntries();
      },
      error: () => {
        this.addingEntry = false;
        this.addEntryError = 'Eintrag konnte nicht gespeichert werden.';
      },
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────
  formatTime(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    const mm = m.toString().padStart(2, '0');
    const ss = s.toString().padStart(2, '0');
    if (h > 0) {
      return `${h}:${mm}:${ss}`;
    }
    return `${mm}:${ss}`;
  }

  parseTime(input: string): number | null {
    const trimmed = (input ?? '').trim();
    if (!trimmed) return null;
    const parts = trimmed.split(':').map(Number);
    if (parts.some((p) => isNaN(p))) return null;
    if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
    if (parts.length === 2) return parts[0] * 60 + parts[1];
    return null;
  }

  entryType(entry: PersonalRecordEntry): string {
    return entry.isManual ? 'Manuell' : 'Automatisch';
  }

  trackEntry(_index: number, entry: PersonalRecordEntry): number {
    return entry.id;
  }

  private reset(): void {
    this.activeTab = 'history';
    this.entries = [];
    this.loadingEntries = false;
    this.entriesError = null;
    this.deletingEntryId = null;
    this.goalTimeInput = this.record?.goalTimeSeconds
      ? this.formatTime(this.record.goalTimeSeconds)
      : '';
    this.savingGoal = false;
    this.goalError = null;
    this.goalSuccess = false;
    this.manualDateInput = '';
    this.manualTimeInput = '';
    this.addingEntry = false;
    this.addEntryError = null;
    this._dirty = false;
  }
}
