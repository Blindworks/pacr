import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { FeedbackService, Feedback, FeedbackStatus } from '../../../../services/feedback.service';

@Component({
  selector: 'app-feedback-list',
  standalone: true,
  imports: [DatePipe, LowerCasePipe, FormsModule, TranslateModule],
  templateUrl: './feedback-list.html',
  styleUrl: './feedback-list.scss'
})
export class FeedbackList implements OnInit {
  private readonly feedbackService = inject(FeedbackService);
  private readonly translate = inject(TranslateService);

  items = signal<Feedback[]>([]);
  isLoading = signal(false);
  selectedStatus = signal<FeedbackStatus | ''>('');
  expandedId = signal<number | null>(null);

  editingStatus: FeedbackStatus = 'NEW';
  editingNotes = '';
  isSaving = signal(false);

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    const status = this.selectedStatus();
    const obs = status ? this.feedbackService.getAll(status) : this.feedbackService.getAll();
    obs.subscribe({
      next: data => { this.items.set(data); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  onStatusFilter(status: string): void {
    this.selectedStatus.set(status as FeedbackStatus | '');
    this.load();
  }

  toggleExpand(item: Feedback): void {
    if (this.expandedId() === item.id) {
      this.expandedId.set(null);
    } else {
      this.expandedId.set(item.id);
      this.editingStatus = item.status;
      this.editingNotes = item.adminNotes || '';
    }
  }

  save(id: number): void {
    this.isSaving.set(true);
    this.feedbackService.update(id, {
      status: this.editingStatus,
      adminNotes: this.editingNotes || null
    }).subscribe({
      next: () => { this.isSaving.set(false); this.expandedId.set(null); this.load(); },
      error: () => this.isSaving.set(false)
    });
  }

  categoryLabel(cat: string): string {
    switch (cat) {
      case 'BUG': return this.translate.instant('ADMIN.CAT_BUG');
      case 'FEATURE_REQUEST': return this.translate.instant('ADMIN.CAT_FEATURE');
      case 'GENERAL': return this.translate.instant('ADMIN.CAT_GENERAL');
      default: return cat;
    }
  }

  statusLabel(s: string): string {
    switch (s) {
      case 'NEW': return this.translate.instant('ADMIN.FEEDBACK_NEW');
      case 'IN_PROGRESS': return this.translate.instant('ADMIN.FEEDBACK_IN_PROGRESS');
      case 'RESOLVED': return this.translate.instant('ADMIN.FEEDBACK_RESOLVED');
      case 'CLOSED': return this.translate.instant('ADMIN.FEEDBACK_CLOSED');
      default: return s;
    }
  }
}
