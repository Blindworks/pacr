import { Component } from '@angular/core';

interface Activity {
  id: number;
  name: string;
  date: string;
  distance: string;
  time: string;
  pace: string;
  elevGain?: string;
  type: 'road' | 'trail' | 'track';
  featured?: boolean;
  mapImage: string;
}

@Component({
  selector: 'app-activities',
  standalone: true,
  templateUrl: './activities.html',
  styleUrl: './activities.scss'
})
export class Activities {
  selectedFilter: string = 'all';
  selectedSort: string = 'recent';

  filters = [
    { id: 'all', label: 'All' },
    { id: 'road', label: 'Road' },
    { id: 'trail', label: 'Trail' },
    { id: 'track', label: 'Track' },
  ];

  activities: Activity[] = [
    {
      id: 1,
      name: 'Sunday Long Run',
      date: 'Oct 29, 2023 • 08:30 AM',
      distance: '19.9 km',
      time: '1:42:05',
      pace: '5\'08" /km',
      elevGain: '257 m',
      type: 'road',
      featured: true,
      mapImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBzhJYjjcQoTEoemxeKBVR33EvCaptA2wIPhERpIqDqbJbPBso3ccYP42UY4nbptfrYJa7cOBhfuMl-5vpF0Gg-9RpJyVEK4bfyrCNWlixF6uP3ExS7gWSRSfmHiMMWpqppyPvdxVZEgawJLwWb5_UiFq1nEMI_pUEYxnRnhy2YSx4NGoUUZhXJHxHNqKICFi-Vso1T1gxLrlG4qf5pYXZP5td4AZkGgzXhz7k2kaDBz_6a1u7A0Rqb4Czizk0a1pe9plcUYJNCtJk'
    },
    {
      id: 2,
      name: 'Trail Recovery',
      date: 'Oct 27, 2023 • 5:15 PM',
      distance: '6.8 km',
      time: '40:12',
      pace: '5\'54" /km',
      type: 'trail',
      mapImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuB3EdaEeJ6PUfNqhhg8dBeJZPHHG2m1x7d7ZFyyLoW1KSueKICw4DBvU1VlKAtGqc19bd_fcjHcPwR9WlI2AfLhQA6-PX-qr0jq-6HHzOApdcxYiF3FnHdaw2JhlvXJw2YTQigsEB4etdv6o9rOaw5_R0Mr4cubnq-6TOFGTKqnjLtMA7Pqjij4gOMK9GgCdZjYodOWQZvDkJt8vLrLgB8cAO5AMsQuCcrT_YaZC72724eQ-kyf_VlRTaQ40CSoK8D13x8fx1gMJHs'
    },
    {
      id: 3,
      name: 'Interval Sprints',
      date: 'Oct 25, 2023 • 07:00 AM',
      distance: '5.0 km',
      time: '22:45',
      pace: '4\'33" /km',
      type: 'track',
      mapImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuA2gYF38D3Da5qQvhLgVwD9WBLx7eCtiUtnfYCalwWEs_fmrRWmA0NhUlx2eGwAYJ1lSC7_e486ZLUf1CYPnVhMhPU60A3xRSMWa4SPVFSDDErTwW4NckFhFQ4TaQ_8gOzNkNgcVN6cn-OB51OjHDpdEH2-B9Xv0qpA1XIWFh3PTjsH1xyZujULhw3C9VDcchtTQxzPABAt1MK58JL8YrhKD0K71HoxuxRJMYErBZcCSlu7UsoexghUGvIDxKe0BbK_x4ua4ojQk7Q'
    },
    {
      id: 4,
      name: 'Evening Commute',
      date: 'Oct 24, 2023 • 6:42 PM',
      distance: '9.3 km',
      time: '48:30',
      pace: '5\'13" /km',
      type: 'road',
      mapImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAhSLEmQhoX6CkmeCJeY1ujwgW-YQSZfky00vBXuFTugss8Oxv_BGtfj4u7tWQnQIasayoD_b6d5Gd7r64w9NQfmzE9oCkvZlxnERpnOlit2vDd8St1KdrxVf7i-Kypbr8rGtEMoegRuNvLTHP8PPAODOq1u5XhymtW3e8HTxrmNM22RPG0SbvmkgDMNvqw05GYYDbX_4pCrh0msot-p7U_m_ZDYHJDb0SQoEi3QITGZhFoTRrlb0-9PwVVuOcKWZT1O01gv3OpxXg'
    },
    {
      id: 5,
      name: 'Elevation Challenge',
      date: 'Oct 22, 2023 • 09:10 AM',
      distance: '10.5 km',
      time: '1:02:15',
      pace: '5\'56" /km',
      type: 'trail',
      mapImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCWjKtZiwDas7wF8HbpQYepfrDwhAwL6nigLEx24wD6EUYb5b4OYA84oiV_uM1g28OOw9W6w5KN4Jberyo0O1LmJ3ecb8cxg1_DugFhqSfmfgTjC29aLtWwaazg3vH0RpTQ_xUoPpN2_Qxebk6WhIZtPuzXIoXHOF99rTn3oaYuhXNbylJdblBAm10rKT5faysFPUBeXse8h5RQ471-72GJYneDfm8VnP_Z5TeQycOU2buKF53IIH5fYSdL3qw_XZEw8YZ-ykvJ0Zw'
    },
  ];

  get featuredActivity(): Activity {
    return this.activities[0];
  }

  get listActivities(): Activity[] {
    return this.activities.slice(1);
  }

  get filteredListActivities(): Activity[] {
    if (this.selectedFilter === 'all') return this.listActivities;
    return this.listActivities.filter(a => a.type === this.selectedFilter);
  }

  showFeatured(): boolean {
    return this.selectedFilter === 'all';
  }

  setFilter(id: string): void {
    this.selectedFilter = id;
  }
}
