import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminBotRunnerService, BotCreateRequest } from '../../../services/admin-bot-runner.service';

const DAYS = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'];

@Component({
  selector: 'app-bot-runner-form',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  templateUrl: './bot-runner-form.html',
  styleUrl: './bot-runner-form.scss'
})
export class BotRunnerForm implements OnInit {
  private botService = inject(AdminBotRunnerService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);

  botId = signal<number | null>(null);
  saving = signal(false);
  error = signal('');

  readonly allDays = DAYS;

  // Identity
  username = signal('');
  email = signal('');
  firstName = signal('');
  lastName = signal('');
  gender = signal<string>('MALE');
  age = signal<number | null>(null);

  // Location
  cityName = signal('');
  homeLatitude = signal<number | null>(48.137154);
  homeLongitude = signal<number | null>(11.576124);
  searchRadiusKm = signal<number>(10);

  // Running profile
  paceMinSecPerKm = signal<number>(270);
  paceMaxSecPerKm = signal<number>(330);
  paceMinDisplay = signal<string>('4:30');
  paceMaxDisplay = signal<string>('5:30');
  distanceMinKm = signal<number>(5);
  distanceMaxKm = signal<number>(12);
  maxHeartRate = signal<number | null>(185);
  restingHeartRate = signal<number | null>(55);

  // Schedule
  selectedDays = signal<Set<string>>(new Set(['MONDAY','WEDNESDAY','FRIDAY']));
  scheduleStartTime = signal<string>('18:00');
  scheduleJitterMinutes = signal<number>(15);

  // Options
  includeInLeaderboard = signal<boolean>(false);
  enabled = signal<boolean>(true);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.botId.set(+id);
      this.botService.get(+id).subscribe({
        next: bot => {
          this.username.set(bot.username);
          this.email.set(bot.email);
          this.firstName.set(bot.firstName || '');
          this.lastName.set(bot.lastName || '');
          this.gender.set(bot.gender || 'MALE');
          this.age.set(bot.age ?? null);
          this.cityName.set(bot.cityName || '');
          this.homeLatitude.set(bot.homeLatitude);
          this.homeLongitude.set(bot.homeLongitude);
          this.searchRadiusKm.set(bot.searchRadiusKm);
          this.paceMinSecPerKm.set(bot.paceMinSecPerKm);
          this.paceMaxSecPerKm.set(bot.paceMaxSecPerKm);
          this.paceMinDisplay.set(this.secToMmss(bot.paceMinSecPerKm));
          this.paceMaxDisplay.set(this.secToMmss(bot.paceMaxSecPerKm));
          this.distanceMinKm.set(bot.distanceMinKm);
          this.distanceMaxKm.set(bot.distanceMaxKm);
          this.maxHeartRate.set(bot.maxHeartRate ?? null);
          this.restingHeartRate.set(bot.restingHeartRate ?? null);
          this.selectedDays.set(new Set(bot.scheduleDays || []));
          if (bot.scheduleStartTime) {
            this.scheduleStartTime.set(bot.scheduleStartTime.substring(0, 5));
          }
          this.scheduleJitterMinutes.set(bot.scheduleJitterMinutes);
          this.includeInLeaderboard.set(bot.includeInLeaderboard);
          this.enabled.set(bot.enabled);
        }
      });
    }
  }

  private secToMmss(sec: number): string {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  private mmssToSec(v: string): number | null {
    const m = /^(\d{1,2}):([0-5]\d)$/.exec(v.trim());
    if (!m) return null;
    return parseInt(m[1], 10) * 60 + parseInt(m[2], 10);
  }

  commitPaceMin(): void {
    const s = this.mmssToSec(this.paceMinDisplay());
    if (s !== null) {
      this.paceMinSecPerKm.set(s);
      this.paceMinDisplay.set(this.secToMmss(s));
    }
  }

  commitPaceMax(): void {
    const s = this.mmssToSec(this.paceMaxDisplay());
    if (s !== null) {
      this.paceMaxSecPerKm.set(s);
      this.paceMaxDisplay.set(this.secToMmss(s));
    }
  }

  toggleDay(day: string): void {
    const set = new Set(this.selectedDays());
    if (set.has(day)) set.delete(day);
    else set.add(day);
    this.selectedDays.set(set);
  }

  isDaySelected(day: string): boolean {
    return this.selectedDays().has(day);
  }

  save(): void {
    if (!this.botId() && (!this.username() || !this.email())) {
      this.error.set(this.translate.instant('ADMIN.BOT_RUNNERS_REQUIRED'));
      return;
    }
    if (this.homeLatitude() === null || this.homeLongitude() === null) {
      this.error.set(this.translate.instant('ADMIN.BOT_RUNNERS_LOCATION_REQUIRED'));
      return;
    }
    if (this.paceMinSecPerKm() > this.paceMaxSecPerKm()
        || this.distanceMinKm() > this.distanceMaxKm()) {
      this.error.set(this.translate.instant('ADMIN.BOT_RUNNERS_RANGE_ERROR'));
      return;
    }

    this.commitPaceMin();
    this.commitPaceMax();
    this.saving.set(true);
    this.error.set('');

    const data: BotCreateRequest = {
      username: this.botId() ? undefined : this.username(),
      email: this.botId() ? undefined : this.email(),
      firstName: this.firstName() || undefined,
      lastName: this.lastName() || undefined,
      cityName: this.cityName() || undefined,
      homeLatitude: this.homeLatitude()!,
      homeLongitude: this.homeLongitude()!,
      searchRadiusKm: this.searchRadiusKm(),
      gender: this.gender() || undefined,
      age: this.age() ?? undefined,
      paceMinSecPerKm: this.paceMinSecPerKm(),
      paceMaxSecPerKm: this.paceMaxSecPerKm(),
      distanceMinKm: this.distanceMinKm(),
      distanceMaxKm: this.distanceMaxKm(),
      maxHeartRate: this.maxHeartRate() ?? undefined,
      restingHeartRate: this.restingHeartRate() ?? undefined,
      scheduleDays: Array.from(this.selectedDays()),
      scheduleStartTime: this.scheduleStartTime() ? `${this.scheduleStartTime()}:00` : undefined,
      scheduleJitterMinutes: this.scheduleJitterMinutes(),
      includeInLeaderboard: this.includeInLeaderboard(),
      enabled: this.enabled()
    };

    const id = this.botId();
    const call = id ? this.botService.update(id, data) : this.botService.create(data);

    call.subscribe({
      next: () => this.router.navigate(['/admin/bot-runners']),
      error: (err) => {
        this.error.set(err?.error?.message || this.translate.instant('ADMIN.BOT_RUNNERS_SAVE_ERROR'));
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/bot-runners']);
  }
}
