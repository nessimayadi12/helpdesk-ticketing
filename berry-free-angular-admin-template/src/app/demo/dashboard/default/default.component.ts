// Angular Import
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

// project import
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { StatsService } from 'src/app/services/stats.service';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-default',
  imports: [CommonModule, RouterModule, SharedModule],
  templateUrl: './default.component.html',
  styleUrls: ['./default.component.scss']
})
export class DefaultComponent {
  isAdmin = false;
  admin: any = null;
  me: any = null;
  meError = false;

  // view models derived from stats for cleaner templates
  adminVM: {
    total: number;
    status: Array<{ name: string; count: number; percent: number }>;
    users: Array<{ username: string; count: number }>;
    latest: Array<any>;
  } | null = null;

  meVM: {
    total: number;
    status: Array<{ name: string; count: number; percent: number }>;
  latest: Array<any>;
  activity: Array<{ date: Date; label: string; count: number; percent: number }>;
  recentUpdated: Array<any>;
  comments: { total: number; top: Array<{ id: number; title: string; count: number }> };
  } | null = null;

  constructor(private stats: StatsService, private auth: AuthService) {
    this.isAdmin = this.auth.getRole() === 'ADMIN';
    if (this.isAdmin) {
      this.stats.admin().subscribe((d) => {
        this.admin = d;
        this.adminVM = this.buildAdminVm(d);
      });
    }
    this.stats.me().subscribe({
      next: (d) => {
        this.me = d;
        this.meVM = this.buildMeVm(d);
      },
      error: () => {
        this.meError = true;
        // minimal empty structure so the UI doesn't stay blank
        this.meVM = {
          total: 0,
          status: [],
          latest: [],
          activity: [],
          recentUpdated: [],
          comments: { total: 0, top: [] }
        };
      }
    });
  }

  private buildStatusList(map: Record<string, number> | undefined, total: number) {
    const entries = Object.entries(map || {});
    return entries
      .map(([name, count]) => ({ name, count, percent: total ? Math.round((count / total) * 100) : 0 }))
      .sort((a, b) => b.count - a.count);
  }

  private buildAdminVm(d: any) {
  // backend returns total as `totalTickets`; keep fallback for older shape `{ totals: { total } }`
  const total = (d?.totalTickets ?? d?.totals?.total) ?? 0;
    const status = this.buildStatusList(d?.byStatus, total);
    const users = Object.entries(d?.byUser || {})
      .map(([username, count]: any) => ({ username, count }))
      .sort((a, b) => b.count - a.count);
    return { total, status, users, latest: d?.latest || [] };
  }

  private buildMeVm(d: any) {
    const total = d?.myTickets ?? 0;
    const status = this.buildStatusList(d?.byStatus, total);
    // recentCreated map: { 'YYYY-MM-DD': count }
    const rc: Record<string, number> = d?.recentCreated || {};
    const entries = Object.entries(rc).sort((a, b) => a[0].localeCompare(b[0]));
    const max = entries.reduce((m, [, c]) => Math.max(m, c as number), 0);
    const activity = entries.map(([iso, count]) => {
      const date = new Date(iso);
      const label = this.formatDay(date);
      const percent = max ? Math.round((Number(count) / max) * 100) : 0;
      return { date, label, count: Number(count), percent };
    });
    return {
      total,
      status,
      latest: d?.latest || [],
      activity,
      recentUpdated: d?.recentUpdated || [],
      comments: d?.comments || { total: 0, top: [] }
    };
  }

  // UI helpers
  statusColor(name: string): string {
    const key = (name || '').toUpperCase();
    switch (key) {
      case 'OPEN':
        return 'primary';
      case 'IN_PROGRESS':
      case 'EN_COURS':
        return 'info';
      case 'PENDING':
      case 'EN_ATTENTE':
        return 'warning';
      case 'RESOLVED':
      case 'RESOLU':
        return 'success';
      case 'CLOSED':
      case 'FERME':
        return 'secondary';
      default:
        return 'dark';
    }
  }

  statusBadgeClass(name: string) {
  const color = this.statusColor(name);
  // solid badge with white text for maximum contrast
  return `badge bg-${color} text-white fw-semibold`;
  }

  progressBarClass(name: string) {
    const color = this.statusColor(name);
    return `progress-bar bg-${color}`;
  }

  private formatDay(d: Date) {
    try {
      // e.g., "lun 27"
      return d.toLocaleDateString('fr-FR', { weekday: 'short', day: '2-digit' });
    } catch {
      return d.toISOString().slice(5, 10); // MM-DD fallback
    }
  }
}
