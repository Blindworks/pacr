import { Component, signal, ViewChild, ElementRef, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ActivityService, CompletedTraining } from '../../services/activity.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './upload.html',
  styleUrl: './upload.scss'
})
export class Upload implements OnInit {
  readonly router = inject(Router);
  private readonly activityService = inject(ActivityService);

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  selectedFile = signal<File | null>(null);
  isDragOver = signal(false);
  isUploading = signal(false);
  uploadError = signal<string | null>(null);
  uploadSuccess = signal(false);
  recentActivities = signal<CompletedTraining[]>([]);

  today = new Intl.DateTimeFormat('sv-SE').format(new Date());

  ngOnInit() {
    this.activityService.getActivities(0, 5).subscribe({
      next: (data) => this.recentActivities.set(data),
      error: () => {}
    });
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragOver.set(true);
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragOver.set(false);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragOver.set(false);
    const file = event.dataTransfer?.files[0];
    if (file) this.validateAndSetFile(file);
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.validateAndSetFile(file);
  }

  private validateAndSetFile(file: File) {
    const name = file.name.toLowerCase();
    if (!name.endsWith('.fit') && !name.endsWith('.gpx') && !name.endsWith('.tcx')) {
      this.uploadError.set('Ungültiges Format. Nur .fit, .gpx und .tcx sind erlaubt.');
      return;
    }
    this.uploadError.set(null);
    this.selectedFile.set(file);
  }

  triggerFilePicker() {
    this.fileInput.nativeElement.click();
  }

  upload() {
    const file = this.selectedFile();
    if (!file) return;
    this.isUploading.set(true);
    this.uploadError.set(null);
    this.activityService.uploadActivity(file, this.today).subscribe({
      next: () => {
        this.isUploading.set(false);
        this.uploadSuccess.set(true);
        setTimeout(() => this.router.navigate(['/activities']), 1200);
      },
      error: (err) => {
        this.isUploading.set(false);
        this.uploadError.set(err?.error?.message || err?.error || 'Upload fehlgeschlagen. Bitte erneut versuchen.');
      }
    });
  }

  formatDate(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatDistance(km: number | null): string {
    if (km == null) return '—';
    return km.toFixed(1) + ' km';
  }

  getSourceLabel(source: string): string {
    if (source === 'FIT_FILE') return 'FIT';
    return source;
  }
}
