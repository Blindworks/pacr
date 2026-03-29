import { Component, ElementRef, ViewChild, effect, inject, OnDestroy } from '@angular/core';
import { AboutDialogService } from '../../services/about-dialog.service';
import { VersionService } from '../../services/version.service';

@Component({
  selector: 'app-about-dialog',
  standalone: true,
  templateUrl: './about-dialog.html',
  styleUrl: './about-dialog.scss'
})
export class AboutDialog implements OnDestroy {
  @ViewChild('dialog') private dialogRef!: ElementRef<HTMLDialogElement>;

  private readonly aboutService = inject(AboutDialogService);
  protected readonly versionService = inject(VersionService);
  protected readonly currentYear = new Date().getFullYear();

  private readonly openEffect = effect(() => {
    const open = this.aboutService.isOpen();
    if (!this.dialogRef) return;
    const dialog = this.dialogRef.nativeElement;
    if (open && !dialog.open) {
      this.versionService.fetch();
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

  ngOnDestroy(): void {}
}
