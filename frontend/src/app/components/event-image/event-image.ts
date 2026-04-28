import { Component, OnChanges, OnDestroy, SimpleChanges, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GroupEventService } from '../../services/group-event.service';

@Component({
  selector: 'app-event-image',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (objectUrl()) {
      <img [src]="objectUrl()" [alt]="alt()" [class]="imgClass()" />
    } @else if (showFallback()) {
      <span class="material-symbols-outlined event-image-fallback">{{ fallbackIcon() }}</span>
    }
  `,
  styles: [`
    :host {
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }
    img {
      display: block;
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .event-image-fallback {
      font-size: 1.75rem;
      color: var(--text-muted);
    }
  `]
})
export class EventImageComponent implements OnChanges, OnDestroy {
  private readonly groupEventService = inject(GroupEventService);

  eventId = input.required<number>();
  filename = input<string | null>(null);
  alt = input<string>('Event image');
  imgClass = input<string>('');
  fallbackIcon = input<string>('event');
  showFallback = input<boolean>(true);

  objectUrl = signal<string | null>(null);
  private currentUrl: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['eventId'] || changes['filename']) {
      this.load();
    }
  }

  private load(): void {
    this.revoke();
    if (!this.filename()) {
      this.objectUrl.set(null);
      return;
    }
    this.groupEventService.getEventImage(this.eventId()).subscribe({
      next: blob => {
        if (!blob) {
          this.objectUrl.set(null);
          return;
        }
        this.currentUrl = URL.createObjectURL(blob);
        this.objectUrl.set(this.currentUrl);
      },
      error: () => this.objectUrl.set(null)
    });
  }

  private revoke(): void {
    if (this.currentUrl) {
      URL.revokeObjectURL(this.currentUrl);
      this.currentUrl = null;
    }
  }

  ngOnDestroy(): void {
    this.revoke();
  }
}
