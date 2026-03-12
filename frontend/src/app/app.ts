import { Component, signal, inject } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Sidebar } from './components/sidebar/sidebar';
import { PaceCalculatorDialog } from './components/pace-calculator/pace-calculator-dialog';
import { filter, map, startWith } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Sidebar, PaceCalculatorDialog],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('frontend');

  private readonly router = inject(Router);

  protected readonly showSidebar = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map((e: NavigationEnd) => !['/login', '/signup', '/forgot-password', '/new-password'].some(p => e.urlAfterRedirects.startsWith(p))),
      startWith(!['/login', '/signup', '/forgot-password', '/new-password'].some(p => this.router.url.startsWith(p)))
    )
  );
}
