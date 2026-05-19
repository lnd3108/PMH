import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TuiButton, TuiInput, TuiNotificationService, TuiTextfield } from '@taiga-ui/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../../service/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TuiButton, TuiInput, TuiTextfield],
  templateUrl: './login-page.html',
  styleUrl: '../auth-page.css',
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(TuiNotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  protected isSubmitting = false;
  protected showPassword = false;

  protected submit(): void {
    if (this.form.invalid || this.isSubmitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    this.authService
      .login(this.form.getRawValue())
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: () => {
          this.notifications
            .open('Đăng nhập thành công', {
              label: 'Thành công',
              appearance: 'positive',
              autoClose: 3000,
            })
            .subscribe();

          const redirectTo = this.route.snapshot.queryParamMap.get('redirectTo');
          const safeRedirect = redirectTo?.startsWith('/') ? redirectTo : '/categories';
          void this.router.navigateByUrl(safeRedirect);
        },
        error: (error) => {
          const message = error?.error?.message || 'Đăng nhập thất bại, vui lòng thử lại!';

          this.notifications
            .open(message, {
              label: 'Lỗi đăng nhập',
              appearance: 'negative',
              autoClose: 5000,
            })
            .subscribe();
        },
      });
  }

  protected togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  protected hasError(controlName: 'email' | 'password', errorCode: string): boolean {
    return this.control(controlName).hasError(errorCode) && this.control(controlName).touched;
  }

  private control(controlName: 'email' | 'password'): AbstractControl {
    return this.form.controls[controlName];
  }
}
