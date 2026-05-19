import { ChangeDetectionStrategy, Component, Injectable, Injector, inject } from '@angular/core';
import { TuiPortalContext } from '@taiga-ui/cdk/portals';
import {
  TuiNotificationOptions,
  TuiNotificationService,
} from '@taiga-ui/core/components/notification';
import { TuiDialogService } from '@taiga-ui/core/portals/dialog';
import { TUI_CONFIRM_DIALOG, TuiConfirmData } from '@taiga-ui/kit/components/confirm';
import { PolymorpheusComponent, injectContext } from '@taiga-ui/polymorpheus';
import { Observable } from 'rxjs';

type NotificationAppearance = 'positive' | 'negative' | 'warning' | 'info';

export interface NotificationAction {
  label: string;
  handler: () => void;
}

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  confirmAppearance?: string;
}

interface UiNotificationData {
  title: string;
  message: string;
  action?: NotificationAction;
}

@Component({
  standalone: true,
  selector: 'app-ui-feedback-notification-content',
  template: `
    <div class="ui-feedback-notification" [attr.data-appearance]="context.appearance">
      <div class="ui-feedback-notification__icon" aria-hidden="true">
        {{ icon }}
      </div>

      <div class="ui-feedback-notification__body">
        <div class="ui-feedback-notification__title">
          {{ context.data.title }}
        </div>

        <div class="ui-feedback-notification__message">
          {{ context.data.message }}
        </div>

        @if (context.data.action; as action) {
          <div class="ui-feedback-notification__actions">
            <button
              type="button"
              class="ui-feedback-notification__action"
              (click)="onAction(action)"
            >
              {{ action.label }}
            </button>
          </div>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UiFeedbackNotificationContentComponent {
  protected readonly context =
    injectContext<TuiPortalContext<TuiNotificationOptions<UiNotificationData>>>();

  protected get icon(): string {
    switch (this.context.appearance) {
      case 'positive':
        return '✓';
      case 'negative':
        return '!';
      case 'warning':
        return '!';
      default:
        return 'i';
    }
  }

  protected onAction(action: NotificationAction): void {
    action.handler();
    this.context.$implicit.complete();
  }
}

@Injectable({
  providedIn: 'root',
})
export class UiFeedbackService {
  private readonly notifications = inject(TuiNotificationService);
  private readonly dialogs = inject(TuiDialogService);
  private readonly confirmDialog = inject(TUI_CONFIRM_DIALOG);
  private readonly injector = inject(Injector);
  private readonly notificationContent = new PolymorpheusComponent(
    UiFeedbackNotificationContentComponent,
    this.injector,
  );

  success(message: string, label = 'Thành công', action?: NotificationAction): void {
    this.notify(message, label, 'positive', action);
  }

  error(message: string, label = 'Lỗi', action?: NotificationAction): void {
    this.notify(message, label, 'negative', action);
  }

  warning(message: string, label = 'Cảnh báo', action?: NotificationAction): void {
    this.notify(message, label, 'warning', action);
  }

  info(message: string, label = 'Thông báo', action?: NotificationAction): void {
    this.notify(message, label, 'info', action);
  }

  confirm(options: ConfirmOptions): Observable<boolean> {
    const data: TuiConfirmData = {
      content: options.message,
      yes: options.confirmText ?? 'Xác nhận',
      no: options.cancelText ?? 'Hủy',
      appearance: options.confirmAppearance ?? 'primary',
    };

    return this.dialogs.open<boolean>(this.confirmDialog, {
      label: options.title,
      size: 's',
      dismissible: true,
      closable: true,
      data,
    });
  }

  private notify(
    message: string,
    label: string,
    appearance: NotificationAppearance,
    action?: NotificationAction,
  ): void {
    this.notifications
      .open(this.notificationContent, {
        label: '',
        appearance,
        autoClose: 3000,
        closable: true,
        icon: '',
        size: 's',
        data: {
          title: label,
          message,
          action,
        },
      })
      .subscribe();
  }
}
