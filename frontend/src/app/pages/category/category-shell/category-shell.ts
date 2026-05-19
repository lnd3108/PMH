import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TuiNotificationService } from '@taiga-ui/core';
import { TuiLanguageName, TuiLanguageSwitcherService } from '@taiga-ui/i18n';
import { AuthService } from '../../../service/auth.service';

type ShellLanguage = 'vietnamese' | 'english';

const SHELL_TRANSLATIONS = {
  vietnamese: {
    navAria: 'Danh mục cấu hình',
    notificationAria: 'Thông báo',
    languageAria: 'Ngôn ngữ',
    fallbackUser: 'Người dùng',
    fallbackRole: 'Người dùng hệ thống',
    logout: 'Đăng xuất',
    logoutSuccess: 'Đăng xuất thành công',
    success: 'Thành công',
    logoutWarning: 'Đăng xuất có cảnh báo',
    logoutFallback: 'Phiên làm việc đã được xóa ở máy khách.',
    menu: [
      'Tham số danh mục theo nhóm',
      'Kênh thanh toán',
      'Mã loại điện tra soát',
      'Tiêu chí dừng phân kênh',
      'Tạm dừng phân kênh',
      'Cấu hình định tuyến kênh',
      'Kênh phân phối/ứng dụng',
      'Tiêu chí chấm điểm cho kênh',
    ],
  },
  english: {
    navAria: 'Configuration menu',
    notificationAria: 'Notifications',
    languageAria: 'Language',
    fallbackUser: 'User',
    fallbackRole: 'System user',
    logout: 'Log out',
    logoutSuccess: 'Logged out successfully',
    success: 'Success',
    logoutWarning: 'Logout warning',
    logoutFallback: 'The client session has already been cleared.',
    menu: [
      'Category parameters by group',
      'Payment channels',
      'Investigation message type codes',
      'Channel stop criteria',
      'Pause channel routing',
      'Channel routing configuration',
      'Distribution channels/applications',
      'Channel scoring criteria',
    ],
  },
} as const;

@Component({
  selector: 'app-category-shell',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './category-shell.html',
  styleUrl: './category-shell.css',
})
export class CategoryShell {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notification = inject(TuiNotificationService);
  private readonly languageSwitcher = inject(TuiLanguageSwitcherService);

  protected readonly user = this.authService.user;
  protected readonly languages: Array<{ code: ShellLanguage; label: string }> = [
    { code: 'vietnamese', label: 'VIE' },
    { code: 'english', label: 'ENG' },
  ];
  protected readonly currentLanguage = signal<ShellLanguage>(
    this.normalizeLanguage(this.languageSwitcher.language),
  );
  protected readonly t = computed(() => SHELL_TRANSLATIONS[this.currentLanguage()]);
  protected isLoggingOut = false;

  protected setLanguage(language: string): void {
    const nextLanguage = this.normalizeLanguage(language);

    this.currentLanguage.set(nextLanguage);
    this.languageSwitcher.setLanguage(nextLanguage as TuiLanguageName);
  }

  protected logout(): void {
    if (this.isLoggingOut) {
      return;
    }

    this.isLoggingOut = true;
    this.authService.logout().subscribe({
      next: () => {
        const text = this.t();

        this.notification
          .open(text.logoutSuccess, {
            label: text.success,
            appearance: 'positive',
            autoClose: 3000,
          })
          .subscribe();
        void this.router.navigate(['/login']);
      },
      error: (error) => {
        const text = this.t();
        const message = error?.error?.message || text.logoutFallback;

        this.notification
          .open(message, {
            label: text.logoutWarning,
            appearance: 'info',
            autoClose: 3000,
          })
          .subscribe();
        void this.router.navigate(['/login']);
      },
      complete: () => {
        this.isLoggingOut = false;
      },
    });
  }

  private normalizeLanguage(language: string): ShellLanguage {
    return language === 'english' ? 'english' : 'vietnamese';
  }
}
