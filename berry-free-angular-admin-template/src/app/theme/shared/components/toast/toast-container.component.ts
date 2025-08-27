import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../service/notification.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast-container.component.html',
  styleUrls: ['./toast-container.component.scss']
})
export class ToastContainerComponent {
  toasts$ = this.notif.toasts$;
  constructor(public notif: NotificationService) {}
  dismiss(id: number) { this.notif.dismiss(id); }
}
