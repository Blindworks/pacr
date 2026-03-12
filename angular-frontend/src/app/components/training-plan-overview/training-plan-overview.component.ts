import { Component, OnInit, OnDestroy, AfterViewChecked, ViewChild, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';

import { ApiService } from '../../services/api.service';
import { Competition, Training, UserTrainingEntry, CompletedTraining, DailyMetrics, WeekSimulationResponse, WeekSimulationWorkout } from '../../models/competition.model';
import { TrainingDetailsDialogComponent } from '../training-details-dialog/training-details-dialog.component';
import { StravaActivityDialogComponent, CompletedTrainingDialogData } from '../strava-activity-dialog/strava-activity-dialog.component';
import { CreateTrainingDialogComponent } from '../create-training-dialog/create-training-dialog.component';
import { Subject, takeUntil, catchError, of, switchMap } from 'rxjs';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { AuthService } from '../../services/auth.service';

Chart.register(...registerables);

interface DayTraining {
  date: string;
  trainings: Training[];
  userEntries: UserTrainingEntry[];
  completedTrainings: CompletedTraining[];
  isEmpty: boolean;
}

interface WeekData {
  weekNumber: number;
  startDate: Date;
  endDate: Date;
  days: DayTraining[];
}

@Component({
  selector: 'app-training-plan-overview',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatBadgeModule,
    MatDividerModule
  ],
  templateUrl: './training-plan-overview.component.html',
  styleUrl: './training-plan-overview.component.scss'
})
export class TrainingPlanOverviewComponent implements OnInit, AfterViewChecked, OnDestroy {
  private destroy$ = new Subject<void>();
  @ViewChild('fatigueCurveCanvas') fatigueCurveCanvas?: ElementRef<HTMLCanvasElement>;
  
  competitions: Competition[] = [];
  selectedCompetitions: number[] = [];
  currentDate = new Date();
  weekData: WeekData | null = null;
  loading = false;
  showEmptyDays = true;
  dailyStrainMap: Map<string, number> = new Map();

  fatigueSimulation: WeekSimulationResponse | null = null;
  fatigueSimulationLoading = false;
  fatigueSimulationError = '';

  private fatigueChart?: Chart;
  private chartRenderPending = false;

  // FIT File Upload
  showFitUploadModal = false;
  selectedFile: File | null = null;
  selectedTrainingForUpload: Training | null = null;
  selectedUploadDate = '';
  dragOver = false;
  isUploading = false;
  uploadProgress = 0;

  // Training type colors
  trainingTypeColors: { [key: string]: string } = {
    speed: '#ff5722',
    endurance: '#2196f3',
    strength: '#9c27b0',
    race: '#ff9800',
    fartlek: '#607d8b',
    recovery: '#4caf50',
    swimming: '#00bcd4',
    cycling: '#795548',
    general: '#9e9e9e'
  };

  // Intensity colors
  intensityColors: { [key: string]: string } = {
    high: '#f44336',
    medium: '#ff9800',
    low: '#ffeb3b',
    recovery: '#4caf50',
    rest: '#9e9e9e'
  };

  dayNames = ['Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag', 'Sonntag'];

  constructor(
    private apiService: ApiService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadCompetitions();
  }

  ngAfterViewChecked(): void {
    if (this.chartRenderPending) {
      this.renderFatigueChart();
      this.chartRenderPending = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.fatigueChart?.destroy();
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    if (event.target && (event.target as HTMLElement).tagName === 'INPUT') {
      return;
    }

    switch (event.key) {
      case 'ArrowLeft':
        event.preventDefault();
        this.goToPreviousWeek();
        break;
      case 'ArrowRight':
        event.preventDefault();
        this.goToNextWeek();
        break;
      case 't':
      case 'T':
        event.preventDefault();
        this.goToToday();
        break;
    }
  }

  loadCompetitions(): void {
    this.apiService.getAllCompetitions()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (competitions) => {
          this.competitions = competitions;
          // Select all competitions by default
          this.selectedCompetitions = competitions.map(c => c.id!);
          this.loadWeekData();
        },
        error: (error) => {
          this.snackBar.open('Fehler beim Laden der WettkÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¤mpfe', 'SchlieÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸en', { duration: 3000 });
        }
      });
  }

  onCompetitionSelectionChange(): void {
    this.loadWeekData();
  }

  loadWeekData(): void {
    this.loading = true;
    const week = this.getWeekDates(this.currentDate);
    const startDate = this.formatDateString(week.startDate);
    const endDate = this.formatDateString(week.endDate);

    this.apiService.getUserCalendarEntries(startDate, endDate)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (entries) => {
          this.processWeekDataFromEntries(week, entries);
          this.loading = false;
        },
        error: () => {
          // Fallback: try old training API
          this.apiService.getTrainingOverview([], '', '')
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: (allTrainings) => {
                this.processWeekData(week, allTrainings);
                this.loading = false;
              },
              error: () => {
                this.snackBar.open('Fehler beim Laden der Trainingsdaten', 'SchlieÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸en', { duration: 3000 });
                this.loading = false;
              }
            });
        }
      });
  }

  private processWeekDataFromEntries(week: { startDate: Date; endDate: Date }, entries: UserTrainingEntry[]): void {
    const mondayOfWeek = this.getMondayOfWeek(this.currentDate);
    const weekData: WeekData = {
      weekNumber: this.getWeekNumber(mondayOfWeek),
      startDate: mondayOfWeek,
      endDate: new Date(mondayOfWeek.getTime() + 6 * 24 * 60 * 60 * 1000),
      days: this.createWeekDays(mondayOfWeek)
    };

    entries.forEach(entry => {
      const dayIndex = weekData.days.findIndex(d => d.date === entry.trainingDate);
      if (dayIndex !== -1) {
        weekData.days[dayIndex].userEntries.push(entry);
        weekData.days[dayIndex].trainings.push(this.mapEntryToTraining(entry));
        weekData.days[dayIndex].isEmpty = false;
      }
    });

    this.syncStravaAndLoadCompleted(weekData);
    this.weekData = weekData;
    this.simulateWeekFatigue(weekData);
  }

  private mapEntryToTraining(entry: UserTrainingEntry): Training {
    return {
      ...entry.training,
      id: entry.id,
      trainingDate: entry.trainingDate,
      date: entry.trainingDate,
      type: entry.training.trainingType,
      intensity: entry.training.intensityLevel,
      duration: entry.training.durationMinutes,
      completed: entry.completed,
      isCompleted: entry.completed,
      completionStatus: entry.completionStatus,
      description: entry.training.description || entry.training.name,
    };
  }

  private processWeekData(week: { startDate: Date; endDate: Date }, allTrainings: Training[]): void {
    const mondayOfWeek = this.getMondayOfWeek(this.currentDate);
    
    const weekData: WeekData = {
      weekNumber: this.getWeekNumber(mondayOfWeek),
      startDate: mondayOfWeek,
      endDate: new Date(mondayOfWeek.getTime() + 6 * 24 * 60 * 60 * 1000), // Sunday
      days: this.createWeekDays(mondayOfWeek)
    };

    this.populateTrainingsForWeek(weekData, allTrainings);
    // First sync Strava activities to DB, then load completed trainings (includes Strava entries)
    this.syncStravaAndLoadCompleted(weekData);

    this.weekData = weekData;
    this.simulateWeekFatigue(weekData);
  }

  private createWeekDays(startDate: Date): DayTraining[] {
    const days: DayTraining[] = [];
    
    for (let i = 0; i < 7; i++) {
      const date = new Date(startDate);
      date.setDate(startDate.getDate() + i);
      
      const dateString = this.formatDateString(date);
      
      days.push({
        date: dateString,
        trainings: [],
        userEntries: [],
        completedTrainings: [],
        isEmpty: true
      });
    }
    
    return days;
  }

  private formatDateString(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private populateTrainingsForWeek(weekData: WeekData, allTrainings: Training[]): void {
    allTrainings.forEach(training => {
      const dayIndex = weekData.days.findIndex(d => d.date === training.trainingDate);
      if (dayIndex !== -1) {
        const mappedTraining = this.mapTrainingForDisplay(training);
        weekData.days[dayIndex].trainings.push(mappedTraining);
        weekData.days[dayIndex].isEmpty = false;
      }
    });
  }

  private mapTrainingForDisplay(training: Training): Training {
    return {
      ...training,
      date: training.trainingDate,
      type: training.trainingType,
      intensity: training.intensityLevel,
      duration: training.durationMinutes,
      completed: training.isCompleted,
      description: training.description || training.name,
      trainingPlanName: training.trainingPlanName,
      trainingPlanId: training.trainingPlanId
    };
  }

  private getMondayOfWeek(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1); // adjust when day is sunday
    const monday = new Date(d.setDate(diff));
    monday.setHours(0, 0, 0, 0);
    return monday;
  }

  private getWeekDates(date: Date): { startDate: Date; endDate: Date } {
    const startDate = this.getMondayOfWeek(date);
    const endDate = new Date(startDate);
    endDate.setDate(startDate.getDate() + 6); // Sunday
    endDate.setHours(23, 59, 59, 999);
    return { startDate, endDate };
  }

  private getWeekNumber(date: Date): number {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil((((d.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
  }

  goToPreviousWeek(): void {
    this.currentDate.setDate(this.currentDate.getDate() - 7);
    this.loadWeekData();
  }

  goToNextWeek(): void {
    this.currentDate.setDate(this.currentDate.getDate() + 7);
    this.loadWeekData();
  }

  goToToday(): void {
    this.currentDate = new Date();
    this.loadWeekData();
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('de-DE', { 
      day: '2-digit', 
      month: '2-digit' 
    });
  }

  formatDayName(dateString: string): string {
    // Parse the date string directly as YYYY-MM-DD
    const [year, month, day] = dateString.split('-').map(Number);
    const date = new Date(year, month - 1, day); // month is 0-indexed in Date constructor
    const dayOfWeek = date.getDay(); // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    
    // Map JavaScript day (0=Sunday) to our array index (0=Monday)  
    const dayNames = ['Sonntag', 'Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag'];
    return dayNames[dayOfWeek];
  }

  isToday(dateString: string): boolean {
    const date = new Date(dateString);
    const today = new Date();
    return date.toDateString() === today.toDateString();
  }

  getTrainingTypeColor(type: string | undefined): string {
    return type ? this.trainingTypeColors[type] || '#9e9e9e' : '#9e9e9e';
  }

  getIntensityColor(intensity: string | undefined): string {
    return intensity ? this.intensityColors[intensity] || '#9e9e9e' : '#9e9e9e';
  }

  isTrainingCompleted(training: Training): boolean {
    return training.completionStatus === 'completed' || !!training.completed;
  }

  isTrainingSkipped(training: Training): boolean {
    return training.completionStatus === 'skipped';
  }

  getTrainingStatusLabel(training: Training): string | null {
    if (this.isTrainingCompleted(training)) {
      return 'Erledigt';
    }

    if (this.isTrainingSkipped(training)) {
      return 'Übersprungen';
    }

    return null;
  }

  formatStartTime(dateTimeString: string): string {
    const isoMatch = /^(\d{4}-\d{2}-\d{2})T(\d{2}):(\d{2})/.exec(dateTimeString);
    if (isoMatch) return `${isoMatch[2]}:${isoMatch[3]}`;
    const date = new Date(dateTimeString);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
  }

  formatDuration(seconds: number): string {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    
    if (hours > 0) {
      return `${hours}h ${minutes}min`;
    } else {
      return `${minutes}min`;
    }
  }

  formatPace(secondsPerKm: number): string {
    const minutes = Math.floor(secondsPerKm / 60);
    const seconds = Math.floor(secondsPerKm % 60);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  getSportName(sport: string | number | undefined): string {
    if (!sport) return 'Training';
    
    // If it's already a string, return it
    if (typeof sport === 'string' && isNaN(Number(sport))) {
      return sport.charAt(0).toUpperCase() + sport.slice(1);
    }
    
    // Map numeric sport IDs to names
    const sportId = typeof sport === 'string' ? parseInt(sport, 10) : sport;
    
    const sportMappings: { [key: number]: string } = {
      1: 'Laufen',
      2: 'Radfahren', 
      3: 'Schwimmen',
      4: 'Transition',
      5: 'Fitness Equipment',
      6: 'Basketball',
      7: 'Soccer',
      8: 'Tennis',
      9: 'American Football',
      10: 'Training',
      11: 'Walking',
      12: 'Cross Country Skiing',
      13: 'Alpine Skiing',
      14: 'Snowboarding',
      15: 'Rowing',
      16: 'Mountaineering',
      17: 'Hiking',
      18: 'Multisport',
      19: 'Paddling',
      20: 'Flying',
      21: 'E-Biking',
      22: 'Motorcycling',
      23: 'Boating',
      24: 'Driving',
      25: 'Golf',
      26: 'Hang Gliding',
      27: 'Horseback Riding',
      28: 'Hunting',
      29: 'Fishing',
      30: 'Inline Skating',
      31: 'Rock Climbing',
      32: 'Sailing',
      33: 'Ice Skating',
      34: 'Sky Diving',
      35: 'Snowshoeing',
      36: 'Snowmobiling',
      37: 'Stand Up Paddleboarding',
      38: 'Surfing',
      39: 'Wakeboarding',
      40: 'Water Skiing',
      41: 'Kayaking',
      42: 'Rafting',
      43: 'Windsurfing',
      44: 'Kitesurfing',
      45: 'Tactical',
      46: 'Jumpmaster',
      47: 'Boxing',
      48: 'Floor Climbing',
      254: 'All'
    };
    
    return sportMappings[sportId] || `Sport ${sportId}`;
  }

  openCompletedTrainingDetails(completed: CompletedTraining): void {
    this.dialog.open(StravaActivityDialogComponent, {
      width: '700px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      panelClass: 'strava-activity-dialog',
      data: { completed } as CompletedTrainingDialogData
    });
  }

  openTrainingDetails(training: Training): void {
    const dialogRef = this.dialog.open(TrainingDetailsDialogComponent, {
      width: '700px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      panelClass: 'training-details-dialog',
      data: { training: training }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.action === 'edit') {
        this.openCreateTrainingDialog(training.trainingDate || training.date!, training);
      }
    });
  }

  openCreateTrainingDialog(date: string, training?: Training): void {
    const dialogRef = this.dialog.open(CreateTrainingDialogComponent, {
      width: '480px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      panelClass: 'create-training-dialog',
      data: { date, training }
    });

    dialogRef.afterClosed().subscribe(saved => {
      if (saved) {
        const msg = training ? 'Training aktualisiert' : 'Training erstellt';
        this.snackBar.open(msg, 'SchlieÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸en', { duration: 2000 });
        this.loadWeekData();
      }
    });
  }

  getCompetitionName(competitionId: number): string {
    const competition = this.competitions.find(c => c.id === competitionId);
    return competition ? competition.name : 'Unbekannt';
  }

  getTotalTrainingsCount(): number {
    if (!this.weekData) return 0;
    return this.weekData.days.reduce((total, day) => total + day.trainings.length, 0);
  }

  getCompletedTrainingsCount(): number {
    if (!this.weekData) return 0;
    return this.weekData.days.reduce((total, day) => total + day.completedTrainings.length, 0);
  }

  getDailyStrain(date: string): number | null {
    const v = this.dailyStrainMap.get(date);
    return v != null ? v : null;
  }

  getStrainColor(strain: number): string {
    if (strain >= 15) return '#ef5350';
    if (strain >= 10) return '#ff7043';
    if (strain >= 6)  return '#ffca28';
    return '#66bb6a';
  }

  private simulateWeekFatigue(weekData: WeekData): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      this.fatigueSimulationLoading = false;
      this.fatigueSimulation = null;
      this.fatigueSimulationError = '';
      this.fatigueChart?.destroy();
      return;
    }

    const workouts = this.mapWeekDataToSimulationWorkouts(weekData);
    this.fatigueSimulationLoading = true;
    this.fatigueSimulationError = '';

    this.apiService.simulateTrainingWeek({
      userId: userId.toString(),
      workouts
    }).pipe(
      takeUntil(this.destroy$),
      catchError(() => {
        this.fatigueSimulationError = 'Fatigue-Simulation konnte nicht geladen werden.';
        return of(null);
      })
    ).subscribe(simulation => {
      this.fatigueSimulationLoading = false;
      this.fatigueSimulation = simulation;
      this.chartRenderPending = true;
    });
  }

  private mapWeekDataToSimulationWorkouts(weekData: WeekData): WeekSimulationWorkout[] {
    return weekData.days.flatMap(day =>
      day.trainings.flatMap(training => {
        const durationMinutes = training.duration ?? training.durationMinutes;
        if (durationMinutes == null || durationMinutes <= 0) {
          return [];
        }

        const zone = this.resolveSimulationZone(training);
        const baseName = training.description || training.name || 'Training';

        return [{
          date: day.date,
          activityName: zone + ' ' + baseName,
          distanceKm: null,
          durationMinutes,
          averagePaceSecondsPerKm: null,
          averageHeartRate: null
        }];
      })
    );
  }

  private resolveSimulationZone(training: Training): 'Z1' | 'Z2' | 'Z3' | 'Z4' | 'Z5' {
    const intensity = (training.intensity || training.intensityLevel || '').toLowerCase();
    if (intensity === 'high') return 'Z4';
    if (intensity === 'medium') return 'Z3';
    if (intensity === 'low') return 'Z2';
    if (intensity === 'recovery' || intensity === 'rest') return 'Z1';

    const text = ((training.description || '') + ' ' + (training.name || '')).toUpperCase();
    const zoneMatch = text.match(/\b(Z[1-5])\b/);
    if (zoneMatch) {
      return zoneMatch[1] as 'Z1' | 'Z2' | 'Z3' | 'Z4' | 'Z5';
    }

    if (text.includes('GA1')) return 'Z2';
    if (text.includes('GA2')) return 'Z3';
    if (text.includes('VO2') || text.includes('INTERVALL') || text.includes('INTERVAL')) return 'Z4';
    if (text.includes('SCHWELLE') || text.includes('TEMPO')) return 'Z4';
    if (text.includes('REGENERATION') || text.includes('RECOVERY') || text.includes('REST')) return 'Z1';

    return 'Z2';
  }

  private extractRiskDate(flag: string): string | null {
    const match = /^(\d{4}-\d{2}-\d{2}):/.exec(flag);
    return match ? match[1] : null;
  }
  private toLocalDateKey(value: string): string {
    const plainDateMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    if (plainDateMatch) {
      return value;
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value.slice(0, 10);
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  formatRiskFlag(flag: string): string {
    const dated = /^(\d{4}-\d{2}-\d{2}):\s*(.*)$/.exec(flag);
    if (dated) {
      const cleaned = dated[2].replace(/_/g, ' ').toLowerCase();
      return dated[1] + ': ' + cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
    }

    const cleaned = flag.replace(/_/g, ' ').toLowerCase();
    return cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
  }

  riskFlagClass(flag: string): string {
    const upper = flag.toUpperCase();
    if (upper.includes('HIGH') || upper.includes('OVERTRAINING')) return 'high';
    if (upper.includes('MEDIUM') || upper.includes('SPIKE')) return 'medium';
    return 'neutral';
  }

  riskFlagStyle(flag: string): Record<string, string> {
    const base: Record<string, string> = {
      display: 'flex',
      alignItems: 'center',
      gap: '8px',
      padding: '6px 10px',
      borderRadius: '8px',
      marginBottom: '6px',
      color: '#dbe7ff',
      background: 'rgba(28, 44, 80, 0.45)'
    };

    const cls = this.riskFlagClass(flag);
    if (cls === 'high') {
      return {
        ...base,
        border: '1px solid rgba(255, 108, 108, 0.4)',
        background: 'rgba(106, 31, 31, 0.36)'
      };
    }
    if (cls === 'medium') {
      return {
        ...base,
        border: '1px solid rgba(255, 185, 98, 0.45)',
        background: 'rgba(105, 74, 18, 0.32)'
      };
    }

    return base;
  }
  riskFlagIcon(flag: string): string {
    const cls = this.riskFlagClass(flag);
    if (cls === 'high') return 'dangerous';
    if (cls === 'medium') return 'warning_amber';
    return 'info';
  }

  private renderFatigueChart(): void {
    const canvas = this.fatigueCurveCanvas?.nativeElement;
    const timeline = this.fatigueSimulation?.fatigueTimeline ?? [];
    const weekDates = this.weekData?.days.map(day => day.date) ?? [];

    if (!canvas || !timeline.length || !weekDates.length || this.fatigueSimulationLoading || !!this.fatigueSimulationError) {
      return;
    }

    this.fatigueChart?.destroy();

    const timelineByDate = new Map<string, number>(
      timeline.map(point => [this.toLocalDateKey(point.date), point.fatigue])
    );

    const labels = weekDates.map(date => `${this.formatDayName(date)} ${this.formatDate(date)}`);
    const values = weekDates.map(date => timelineByDate.get(date) ?? null);
    const numericValues = values.filter((value): value is number => value != null);
    if (!numericValues.length) {
      return;
    }

    const peak = this.fatigueSimulation?.peakFatigue ?? Math.max(...numericValues);

    const riskDates = new Set(
      (this.fatigueSimulation?.riskFlags ?? [])
        .map(flag => this.extractRiskDate(flag))
        .filter((date): date is string => !!date)
    );

    const riskIndexSet = new Set(
      weekDates
        .map((date, index) => ({ date, index }))
        .filter(item => riskDates.has(item.date))
        .map(item => item.index)
    );

    this.fatigueChart = new Chart(canvas, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Fatigue Curve',
          data: values as number[],
          borderColor: '#2d7bff',
          backgroundColor: 'rgba(45, 123, 255, 0.2)',
          tension: 0.28,
          borderWidth: 2,
          pointRadius: values.map((value, index) => (value != null && (value === peak || riskIndexSet.has(index)) ? 5 : 3)),
          pointHoverRadius: values.map((value, index) => (value != null && (value === peak || riskIndexSet.has(index)) ? 7 : 5)),
          pointBackgroundColor: values.map((value, index) => {
            if (riskIndexSet.has(index)) return '#ef5350';
            if (value === peak) return '#ffb300';
            return '#2d7bff';
          }),
          pointBorderColor: values.map((value, index) => {
            if (riskIndexSet.has(index)) return '#ffcdd2';
            if (value === peak) return '#ffd54f';
            return '#d7e7ff';
          }),
          pointBorderWidth: values.map((value, index) => (value != null && (value === peak || riskIndexSet.has(index)) ? 2 : 1)),
          fill: true
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        interaction: {
          mode: 'index',
          intersect: false
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#111a2e',
            borderColor: 'rgba(80, 102, 140, 0.55)',
            borderWidth: 1,
            titleColor: '#e9efff',
            bodyColor: '#d6e2ff',
            displayColors: false,
            padding: 10,
            callbacks: {
              title: (items) => {
                const index = items[0]?.dataIndex;
                if (index == null) return '';
                const date = weekDates[index];
                return date ? `${this.formatDayName(date)}, ${this.formatDate(date)}` : '';
              },
              label: (context) => {
                const raw = context.raw as number | null;
                if (raw == null) return 'Fatigue: -';
                return 'Fatigue: ' + (raw * 100).toFixed(0) + '%';
              }
            }
          }
        },
        layout: {
          padding: { top: 6, right: 6, bottom: 2, left: 4 }
        },
        scales: {
          y: {
            beginAtZero: true,
            max: 1,
            ticks: {
              color: '#9fb0c4',
              callback: (value) => Math.round(Number(value) * 100) + '%'
            },
            title: {
              display: true,
              text: 'Fatigue',
              color: '#9fb0c4'
            },
            grid: {
              color: 'rgba(120, 137, 160, 0.25)'
            }
          },
          x: {
            ticks: { color: '#9fb0c4' },
            title: {
              display: true,
              text: 'Tage',
              color: '#9fb0c4'
            },
            grid: { color: 'rgba(120, 137, 160, 0.08)' }
          }
        }
      } as ChartConfiguration<'line'>['options']
    });
  }

  private syncStravaAndLoadCompleted(weekData: WeekData): void {
    const startDate = weekData.days[0].date;
    const endDate = weekData.days[6].date;

    // Sync Strava to DB first, then load all completed trainings (including newly synced Strava entries)
    this.apiService.getStravaActivities(startDate, endDate).pipe(
      takeUntil(this.destroy$),
      catchError(() => of([])),
      switchMap(() => this.apiService.getCompletedTrainingsByDateRange(startDate, endDate).pipe(
        catchError(() => of([]))
      ))
    ).subscribe(allCompletedTrainings => {
      // Clear existing completed trainings before repopulating
      weekData.days.forEach(d => { d.completedTrainings = []; });

      allCompletedTrainings.forEach(training => {
        const day = weekData.days.find(d => d.date === training.trainingDate);
        if (day) {
          day.completedTrainings.push(training);
          day.isEmpty = false;
        }
      });

      this.weekData = { ...weekData };

      // Load daily strain for the week
      this.apiService.getDailyMetrics(startDate, endDate).pipe(
        catchError(() => of([]))
      ).subscribe((metrics: DailyMetrics[]) => {
        this.dailyStrainMap = new Map(
          metrics
            .filter(m => m.dailyStrain21 != null)
            .map(m => [m.date, m.dailyStrain21!])
        );
      });
    });
  }
  // Training Completion Methods
  setTrainingCompletionStatus(training: Training, status: 'completed' | 'skipped'): void {
    const isSameStatus = training.completionStatus === status;
    const nextStatus = isSameStatus ? 'pending' : status;
    const feedback = {
      completed: nextStatus === 'completed',
      completionStatus: nextStatus
    };

    // Use UserTrainingEntry feedback endpoint (new architecture)
    this.apiService.updateTrainingEntryFeedback(training.id!, feedback).pipe(
      takeUntil(this.destroy$),
      catchError(() => this.apiService.updateTrainingFeedback(training.id!, feedback))
    ).subscribe({
      next: () => {
        training.completed = feedback.completed;
        training.isCompleted = feedback.completed;
        training.completionStatus = nextStatus;
        const message = nextStatus === 'completed'
          ? 'Training als erledigt markiert'
          : nextStatus === 'skipped'
            ? 'Training als übersprungen markiert'
            : 'Training wieder als offen markiert';
        this.snackBar.open(message, 'SchlieÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸en', { duration: 2000 });
        this.loadWeekData();
      },
      error: () => {
        this.snackBar.open('Fehler beim Aktualisieren des Training-Status', 'SchlieÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸en', { duration: 3000 });
      }
    });
  }

  // FIT File Upload Methods
  openFitFileUpload(training: Training, date: string): void {
    this.selectedTrainingForUpload = training;
    this.selectedUploadDate = date;
    this.showFitUploadModal = true;
    this.selectedFile = null;
    this.uploadProgress = 0;
  }

  closeFitUploadModal(): void {
    this.showFitUploadModal = false;
    this.selectedTrainingForUpload = null;
    this.selectedUploadDate = '';
    this.selectedFile = null;
    this.dragOver = false;
    this.isUploading = false;
    this.uploadProgress = 0;
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.selectFile(files[0]);
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectFile(file);
    }
  }

  selectFile(file: File): void {
    if (!file.name.toLowerCase().endsWith('.fit')) {
      this.snackBar.open('Bitte wÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¤hlen Sie eine FIT-Datei aus', 'SchlieÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸en', { duration: 3000 });
      return;
    }
    this.selectedFile = file;
  }

  removeFile(): void {
    this.selectedFile = null;
  }

  uploadFitFile(): void {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.uploadProgress = 0;

    const formData = new FormData();
    formData.append('file', this.selectedFile);
    if (this.selectedUploadDate) {
      formData.append('date', this.selectedUploadDate);
    }
    if (this.selectedTrainingForUpload?.id) {
      formData.append('trainingId', this.selectedTrainingForUpload.id.toString());
    }

    // Simulate progress for demo
    const progressInterval = setInterval(() => {
      this.uploadProgress += 10;
      if (this.uploadProgress >= 90) {
        clearInterval(progressInterval);
      }
    }, 200);

    this.apiService.uploadFitFile(formData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          clearInterval(progressInterval);
          this.uploadProgress = 100;
          this.snackBar.open('FIT-Datei erfolgreich hochgeladen', 'SchlieÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸en', { duration: 3000 });
          this.closeFitUploadModal();
          this.loadWeekData(); // Reload to show completed training
        },
        error: (error) => {
          clearInterval(progressInterval);
          this.isUploading = false;
          this.uploadProgress = 0;
          
          console.error('FIT File upload error:', error);
          console.log('FormData contents:', {
            file: this.selectedFile?.name,
            date: this.selectedUploadDate,
            fileSize: this.selectedFile?.size
          });
          
          let errorMessage = 'FIT-Datei konnte nicht verarbeitet werden';
          if (error.error) {
            if (typeof error.error === 'string') {
              errorMessage = error.error;
            } else if (error.error.message) {
              errorMessage = error.error.message;
            }
          }
          
          this.snackBar.open('Fehler beim Upload: ' + errorMessage, 'SchlieÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸en', { duration: 5000 });
        }
      });
  }

}





