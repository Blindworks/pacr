import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminLadvService } from '../../../../services/admin-ladv.service';
import { LadvCreateSourceRequest, LadvLvOption } from '../../../../models/ladv.model';

@Component({
  selector: 'app-ladv-source-form',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  templateUrl: './ladv-source-form.html',
  styleUrl: './ladv-source-form.scss'
})
export class LadvSourceForm implements OnInit {
  private service = inject(AdminLadvService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);

  sourceId = signal<number | null>(null);
  name = signal('');
  lv = signal('');
  bestenlistenfaehigOnly = signal(false);
  enabled = signal(true);
  saving = signal(false);
  error = signal('');
  lvOptions = signal<LadvLvOption[]>([]);

  ngOnInit(): void {
    this.service.listLvs().subscribe({
      next: opts => this.lvOptions.set(opts)
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.sourceId.set(+id);
      this.service.getSource(+id).subscribe({
        next: item => {
          this.name.set(item.name);
          this.lv.set(item.lv);
          this.bestenlistenfaehigOnly.set(item.bestenlistenfaehigOnly);
          this.enabled.set(item.enabled);
        }
      });
    }
  }

  save(): void {
    if (!this.name().trim() || !this.lv().trim()) {
      this.error.set(this.translate.instant('ADMIN.LADV_SOURCE_REQUIRED'));
      return;
    }
    this.saving.set(true);
    this.error.set('');

    const data: LadvCreateSourceRequest = {
      name: this.name().trim(),
      lv: this.lv().trim(),
      bestenlistenfaehigOnly: this.bestenlistenfaehigOnly(),
      enabled: this.enabled()
    };
    const id = this.sourceId();
    const call = id ? this.service.updateSource(id, data) : this.service.createSource(data);

    call.subscribe({
      next: () => this.router.navigate(['/admin/ladv']),
      error: err => {
        this.error.set(err?.error?.message
          ?? this.translate.instant('ADMIN.LADV_SAVE_ERROR'));
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/ladv']);
  }
}
