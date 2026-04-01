import { Component, inject } from '@angular/core';
import { FeedbackDialogService } from '../../services/feedback-dialog.service';

@Component({
  selector: 'app-feedback-fab',
  standalone: true,
  templateUrl: './feedback-fab.html',
  styleUrl: './feedback-fab.scss'
})
export class FeedbackFab {
  private readonly dialogService = inject(FeedbackDialogService);
  openFeedback(): void { this.dialogService.open(); }
}
