import { Component, ElementRef, ViewChild, effect, inject, signal, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AboutDialogService } from '../../services/about-dialog.service';
import { VersionService } from '../../services/version.service';
import { apiUrl } from '../../core/api-base';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-about-dialog',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './about-dialog.html',
  styleUrl: './about-dialog.scss'
})
export class AboutDialog implements OnDestroy {
  @ViewChild('dialog') private dialogRef!: ElementRef<HTMLDialogElement>;

  private readonly aboutService = inject(AboutDialogService);
  private readonly http = inject(HttpClient);
  protected readonly versionService = inject(VersionService);
  protected readonly currentYear = new Date().getFullYear();

  protected readonly showChangelog = signal(false);
  protected readonly changelogHtml = signal<string>('');
  protected readonly changelogLoading = signal(false);
  protected readonly showDisclaimer = signal(false);

  private readonly openEffect = effect(() => {
    const open = this.aboutService.isOpen();
    if (!this.dialogRef) return;
    const dialog = this.dialogRef.nativeElement;
    if (open && !dialog.open) {
      this.versionService.fetch();
      this.showChangelog.set(false);
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  });

  close(): void {
    this.aboutService.close();
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogRef.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom;
    if (outside) this.close();
  }

  toggleChangelog(): void {
    if (this.showChangelog()) {
      this.showChangelog.set(false);
      return;
    }
    if (!this.changelogHtml()) {
      this.loadChangelog();
    }
    this.showChangelog.set(true);
  }

  toggleDisclaimer(): void {
    this.showDisclaimer.set(!this.showDisclaimer());
  }

  private loadChangelog(): void {
    this.changelogLoading.set(true);
    this.http.get(apiUrl('/changelog'), { responseType: 'text' }).subscribe({
      next: md => {
        this.changelogHtml.set(this.parseMarkdown(md));
        this.changelogLoading.set(false);
      },
      error: () => {
        this.changelogHtml.set('<p>Changelog could not be loaded.</p>');
        this.changelogLoading.set(false);
      }
    });
  }

  private parseMarkdown(md: string): string {
    const lines = md.split('\n');
    const html: string[] = [];
    let inList = false;

    for (const line of lines) {
      const trimmed = line.trim();

      if (!trimmed) {
        if (inList) { html.push('</ul>'); inList = false; }
        continue;
      }

      // Skip the main title
      if (trimmed.startsWith('# ') && !trimmed.startsWith('## ')) continue;

      // Version headings
      if (trimmed.startsWith('## ')) {
        if (inList) { html.push('</ul>'); inList = false; }
        const text = this.escapeHtml(trimmed.replace(/^## /, ''));
        html.push(`<h3 class="cl-version">${text}</h3>`);
        continue;
      }

      // Category headings (Added, Changed, Fixed, Removed)
      if (trimmed.startsWith('### ')) {
        if (inList) { html.push('</ul>'); inList = false; }
        const text = this.escapeHtml(trimmed.replace(/^### /, ''));
        html.push(`<h4 class="cl-category">${text}</h4>`);
        continue;
      }

      // List items
      if (trimmed.startsWith('- ')) {
        if (!inList) { html.push('<ul class="cl-list">'); inList = true; }
        const text = this.escapeHtml(trimmed.replace(/^- /, ''));
        html.push(`<li>${text}</li>`);
        continue;
      }
    }

    if (inList) html.push('</ul>');
    return html.join('\n');
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  ngOnDestroy(): void {}
}
