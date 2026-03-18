import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgClass } from '@angular/common';
import { PaceCalculatorService } from '../../services/pace-calculator.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgClass],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar implements OnInit {
  bodyDataOpen = false;

  private readonly paceCalc = inject(PaceCalculatorService);
  protected readonly userService = inject(UserService);

  ngOnInit(): void {
    // Laden falls noch kein anderer Aufrufer (z.B. Settings) den State befüllt hat
    if (!this.userService.currentUser()) {
      this.userService.getMe().subscribe({ error: () => {} });
    }
  }

  toggleBodyData(): void {
    this.bodyDataOpen = !this.bodyDataOpen;
  }

  openPaceCalculator(): void {
    this.paceCalc.open();
  }
}
