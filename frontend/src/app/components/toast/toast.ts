import { Component, inject } from '@angular/core';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    @for (toast of toastService.toasts(); track toast.id) {
      <div class="toast" [class]="'toast--' + toast.type" (click)="toastService.dismiss(toast.id)">
        @if (toast.type === 'achievement') {
          <div class="toast-badge">
            <span class="material-symbols-outlined toast-badge-icon">{{ toast.icon ?? 'military_tech' }}</span>
          </div>
          <div class="toast-content">
            <span class="toast-label">Achievement Unlocked!</span>
            <span class="toast-message">{{ toast.message }}</span>
          </div>
        } @else {
          <span class="material-symbols-outlined toast-icon">
            {{ toast.type === 'success' ? 'check_circle' : 'info' }}
          </span>
          <span class="toast-message">{{ toast.message }}</span>
        }
        <button class="toast-close" (click)="toastService.dismiss(toast.id)">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
    }
  `,
  styles: [`
    :host {
      position: fixed;
      top: 1.5rem;
      right: 1.5rem;
      z-index: 10000;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      pointer-events: none;
    }

    .toast {
      pointer-events: auto;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.875rem 1.25rem;
      border-radius: 12px;
      background: #161b22;
      border: 1px solid #30363d;
      color: #e6edf3;
      font-family: 'Lexend', 'Inter', sans-serif;
      font-size: 0.875rem;
      min-width: 300px;
      max-width: 420px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
      cursor: pointer;
      animation: slideIn 0.3s ease-out;
    }

    .toast--achievement {
      border-color: #f59e0b;
      background: linear-gradient(135deg, #1c1a0e 0%, #161b22 100%);
    }

    .toast--success {
      border-color: #238636;
    }

    .toast-badge {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: linear-gradient(135deg, #f59e0b, #d97706);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .toast-badge-icon {
      font-size: 22px;
      color: #fff;
    }

    .toast-content {
      display: flex;
      flex-direction: column;
      gap: 0.125rem;
    }

    .toast-label {
      font-size: 0.7rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #f59e0b;
      font-weight: 600;
    }

    .toast-message {
      font-weight: 500;
    }

    .toast-icon {
      font-size: 20px;
      color: #58a6ff;
      flex-shrink: 0;
    }

    .toast--success .toast-icon {
      color: #3fb950;
    }

    .toast-close {
      background: none;
      border: none;
      color: #484f58;
      cursor: pointer;
      padding: 2px;
      margin-left: auto;
      flex-shrink: 0;
    }

    .toast-close .material-symbols-outlined {
      font-size: 18px;
    }

    .toast-close:hover {
      color: #e6edf3;
    }

    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }
  `]
})
export class Toast {
  protected readonly toastService = inject(ToastService);
}
