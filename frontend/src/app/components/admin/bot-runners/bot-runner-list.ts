import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { AdminBotRunnerService, BotProfileDto } from '../../../services/admin-bot-runner.service';

@Component({
  selector: 'app-bot-runner-list',
  standalone: true,
  imports: [DatePipe, TranslateModule],
  templateUrl: './bot-runner-list.html',
  styleUrl: './bot-runner-list.scss'
})
export class BotRunnerList implements OnInit {
  private botService = inject(AdminBotRunnerService);
  private router = inject(Router);

  bots = signal<BotProfileDto[]>([]);
  isLoading = signal(false);
  runningId = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.botService.getAll().subscribe({
      next: data => { this.bots.set(data); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  goNew(): void {
    this.router.navigate(['/admin/bot-runners/new']);
  }

  edit(bot: BotProfileDto): void {
    this.router.navigate(['/admin/bot-runners', bot.id, 'edit']);
  }

  runNow(bot: BotProfileDto): void {
    this.runningId.set(bot.id);
    this.botService.runNow(bot.id).subscribe({
      next: () => { this.runningId.set(null); this.load(); },
      error: () => this.runningId.set(null)
    });
  }

  delete(bot: BotProfileDto): void {
    if (!confirm(`Bot "${bot.username}" wirklich löschen?`)) return;
    this.botService.delete(bot.id).subscribe({ next: () => this.load() });
  }

  formatSchedule(bot: BotProfileDto): string {
    if (!bot.scheduleDays || bot.scheduleDays.length === 0) return '—';
    const days = bot.scheduleDays.map(d => d.substring(0, 3)).join(',');
    const time = bot.scheduleStartTime ? bot.scheduleStartTime.substring(0, 5) : '';
    return `${days} ${time}`.trim();
  }

  formatPace(bot: BotProfileDto): string {
    return `${this.secToPace(bot.paceMinSecPerKm)}–${this.secToPace(bot.paceMaxSecPerKm)}`;
  }

  private secToPace(sec: number): string {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }
}
