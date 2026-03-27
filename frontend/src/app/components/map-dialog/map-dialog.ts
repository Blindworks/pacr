import { Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GpsStreamDto } from '../../services/activity.service';
import { ActivityMapComponent } from '../activity-map/activity-map';

@Component({
  selector: 'app-map-dialog',
  standalone: true,
  imports: [CommonModule, ActivityMapComponent],
  templateUrl: './map-dialog.html',
  styleUrl: './map-dialog.scss'
})
export class MapDialogComponent {
  @ViewChild('dialog') private dialogRef!: ElementRef<HTMLDialogElement>;

  gpsData: GpsStreamDto | null = null;
  colorMode: 'pace' | 'hr' = 'pace';

  open(data: GpsStreamDto, mode: 'pace' | 'hr' = 'pace'): void {
    this.gpsData = data;
    this.colorMode = mode;
    this.dialogRef.nativeElement.showModal();
  }

  close(): void {
    this.dialogRef.nativeElement.close();
    this.gpsData = null;
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogRef.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom;
    if (outside) this.close();
  }
}
