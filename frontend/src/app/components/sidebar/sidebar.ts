import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgClass } from '@angular/common';
import { PaceCalculatorService } from '../../services/pace-calculator.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgClass],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar {
  bodyDataOpen = false;

  private readonly paceCalc = inject(PaceCalculatorService);

  toggleBodyData(): void {
    this.bodyDataOpen = !this.bodyDataOpen;
  }

  openPaceCalculator(): void {
    this.paceCalc.open();
  }
}
