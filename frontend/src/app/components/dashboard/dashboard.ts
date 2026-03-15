import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardData } from '../../services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  data: DashboardData | null = null;

  ngOnInit(): void {
    console.log('ngOnInit called');
    this.dashboardService.getDashboard().subscribe({
      next: data => {
        console.log('data received', data.readinessScore);
        this.data = data;
      },
      error: err => console.error('Dashboard load failed', err)
    });
  }
}
