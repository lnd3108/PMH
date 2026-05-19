import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TuiButton, TuiInput, TuiNotificationService, TuiTextfield } from '@taiga-ui/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../../service/auth.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TuiInput, TuiTextfield, TuiButton],
  templateUrl: './register-page.html',
  styleUrl: '../auth-page.css',
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(TuiNotificationService);

  protected readonly form = this.fb.nonNullable.group(
    {
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: [this.passwordMatchValidator] },
  );

  protected isSubmitting = false;
  protected showPassword = false;
  protected showConfirmPassword = false;

  protected submit(): void {
    if (this.form.invalid || this.isSubmitting) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, email, password } = this.form.getRawValue();
    this.isSubmitting = true;

    this.authService
      .register({ name, email, password })
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: () => {
          this.notifications
            .open('Đăng ký thành công, bạn có thể đăng nhập ngay bây giờ!', {
              label: 'Thành công',
              appearance: 'positive',
              autoClose: 3000,
            })
            .subscribe();

          void this.router.navigate(['/login']);
        },
        error: (error) => {
          const message = error?.error?.message || 'Đăng ký thất bại, vui lòng thử lại!';

          this.notifications
            .open(message, {
              label: 'Lỗi',
              appearance: 'negative',
              autoClose: 3000,
            })
            .subscribe();
        },
      });
  }

  protected togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  protected toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  protected hasError(
    controlName: 'name' | 'email' | 'password' | 'confirmPassword',
    errorCode: string,
  ): boolean {
    return this.control(controlName).hasError(errorCode) && this.control(controlName).touched;
  }

  protected hasFormError(errorCode: string): boolean {
    return this.form.hasError(errorCode) && this.form.touched;
  }

  private control(controlName: 'name' | 'email' | 'password' | 'confirmPassword'): AbstractControl {
    return this.form.controls[controlName];
  }

  private passwordMatchValidator(group: AbstractControl) {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;

    if (!password || !confirmPassword) {
      return null;
    }

    return password === confirmPassword ? null : { passwordMismatch: true };
  }
}
