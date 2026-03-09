import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

type Integration = {
  id: string;
  name: string;
  description: string;
  iconBg: string;
  connected: boolean;
};

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.scss'
})
export class Settings {
  protected weight = signal('72');
  protected height = signal('180');
  protected age = signal('28');
  protected gender = signal('Male');
  protected restingHr = signal('54');

  protected unit = signal<'metric' | 'imperial'>('metric');
  protected theme = signal<'light' | 'dark' | 'auto'>('dark');
  protected pushNotifications = signal(true);
  protected emailDigest = signal(false);

  protected readonly integrations: Integration[] = [
    {
      id: 'strava',
      name: 'Strava',
      description: 'Sync activities and segments automatically',
      iconBg: '#FC6100',
      connected: false
    },
    {
      id: 'garmin',
      name: 'Garmin Connect',
      description: 'Import health metrics and training data',
      iconBg: '#0076C0',
      connected: true
    },
    {
      id: 'apple',
      name: 'Apple Health',
      description: 'Sync steps, sleep, and heart rate',
      iconBg: '#1c1c1e',
      connected: false
    }
  ];

  protected setUnit(value: 'metric' | 'imperial'): void {
    this.unit.set(value);
  }

  protected setTheme(value: 'light' | 'dark' | 'auto'): void {
    this.theme.set(value);
  }

  protected toggleIntegration(integration: Integration): void {
    integration.connected = !integration.connected;
  }

  protected saveChanges(): void {
    // TODO: wire to backend
    console.log('Settings saved');
  }
}
