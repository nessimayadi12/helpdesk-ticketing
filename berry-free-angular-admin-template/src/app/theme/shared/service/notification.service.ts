import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastType = 'success' | 'info' | 'error';
export interface Toast { id: number; type: ToastType; message: string; timeout?: number; }

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private toastsSubject = new BehaviorSubject<Toast[]>([]);
  toasts$ = this.toastsSubject.asObservable();
  private idSeq = 1;

  private push(type: ToastType, message: string, timeout = 3000) {
    const toast: Toast = { id: this.idSeq++, type, message, timeout };
    const list = [...this.toastsSubject.value, toast];
    this.toastsSubject.next(list);
    if (timeout > 0) {
      setTimeout(() => this.dismiss(toast.id), timeout);
    }
  }

  success(msg: string, timeout = 3000) { this.push('success', msg, timeout); }
  info(msg: string, timeout = 3000) { this.push('info', msg, timeout); }
  error(msg: string, timeout = 4000) { this.push('error', msg, timeout); }

  dismiss(id: number) {
    this.toastsSubject.next(this.toastsSubject.value.filter(t => t.id !== id));
  }
}
