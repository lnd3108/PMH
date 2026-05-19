import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { TuiDay } from '@taiga-ui/cdk';
import { TuiChevron, TuiInputDate } from '@taiga-ui/kit';

import { UiFeedbackService } from '../../../core/ui-feedback.service';
import { CategoryStatus, CategoryStatusPolicy } from '../../../domain/category/category-status';
import { Category } from '../../../models/category.models';
import { CategoryService } from '../../../service/category.service';
import {
  TuiButton,
  TuiCalendar,
  TuiDateFormat,
  TuiDropdown,
  TuiInput,
  TuiLabel,
  TuiTextfield,
} from '@taiga-ui/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MaskitoDirective } from '@maskito/angular';
import type { MaskitoOptions } from '@maskito/core';
import { parseCategoryNewData } from '../../../domain/category/category-new-data';

export type CategoryFormMode = 'create' | 'edit' | 'copy';

@Component({
  selector: 'app-category-form-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TuiButton,
    TuiInput,
    TuiTextfield,
    TuiLabel,
    TuiDropdown,
    TuiInputDate,
    TuiCalendar,
    TuiChevron,
    TuiDateFormat,
    MaskitoDirective,
  ],
  templateUrl: './category-form-page.html',
  styleUrl: './category-form-page.css',
})
export class CategoryFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ui = inject(UiFeedbackService);
  private readonly categoryService = inject(CategoryService);

  @Input() mode: CategoryFormMode = 'create';
  @Input() id: number | null = null;
  @Input() dialogMode = false;
  @Output() completed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  componentCodeDropdownOpen = false;
  private componentCodeDropdownOpenBeforeTriggerClick = false;
  private ignoreComponentCodeDropdownOpenChange = false;
  loading = false;
  saving = false;
  currentStatus: number = CategoryStatus.New;
  readonlyForm = false;

  componentOptions: string[] = [];
  componentOptionsLoading = false;
  componentOptionsLoadFailed = false;

  readonly mask: MaskitoOptions = {
    mask: /^[a-zA-Z0-9_]*$/,
  };
  readonly maskTiengViet: MaskitoOptions = {
    mask: /^[\p{L}\p{N}\s]*$/u,
  };

  form = this.fb.group(
    {
      paramName: this.fb.nonNullable.control('', Validators.required),
      paramValue: this.fb.nonNullable.control('', Validators.required),
      paramType: this.fb.nonNullable.control('', Validators.required),
      componentCode: this.fb.nonNullable.control('', Validators.required),

      effectiveDate: new FormControl<TuiDay | null>(null, {
        validators: [Validators.required, this.notPastDateValidator()],
      }),

      endEffectiveDate: new FormControl<TuiDay | null>(null, {
        validators: [this.notPastDateValidator()],
      }),

      description: this.fb.nonNullable.control(''),

      status: this.fb.nonNullable.control(CategoryStatus.New),
      isActive: this.fb.nonNullable.control(1),
      isDisplay: this.fb.nonNullable.control(1),
    },
    {
      validators: [this.endDateAfterEffectiveDateValidator()],
    },
  );

  ngOnInit(): void {
    if (!this.dialogMode) {
      this.mode = (this.route.snapshot.data['mode'] ?? 'create') as CategoryFormMode;

      const idParam = this.route.snapshot.paramMap.get('id');
      this.id = idParam ? Number(idParam) : null;
    }

    this.form.controls.effectiveDate.valueChanges.subscribe(() => {
      this.form.updateValueAndValidity({ emitEvent: false });
    });

    this.form.controls.endEffectiveDate.valueChanges.subscribe(() => {
      this.form.updateValueAndValidity({ emitEvent: false });
    });

    this.loadComponentOptions();

    if (this.mode !== 'create' && this.id) {
      this.loadDetail(this.id);
      return;
    }

    this.applyReadonlyState();
  }

  loadDetail(id: number): void {
    this.loading = true;

    this.categoryService.getById(id).subscribe({
      next: (data) => {
        const status =
          this.mode === 'copy' ? CategoryStatus.New : (data.status ?? CategoryStatus.New);
        const displayData = this.mode === 'edit' ? this.mergeNewDataPreview(data) : data;

        this.currentStatus = status;

        this.form.patchValue({
          paramName: this.mode === 'copy' ? '' : (displayData.paramName ?? ''),
          paramValue: displayData.paramValue ?? '',
          paramType: displayData.paramType ?? '',
          componentCode: displayData.componentCode ?? '',
          effectiveDate: this.toTuiDay(displayData.effectiveDate),
          endEffectiveDate: this.toTuiDay(displayData.endEffectiveDate),
          description: displayData.description ?? '',
          status,
          isActive: displayData.isActive ?? 1,
          isDisplay: this.mode === 'copy' ? 1 : (displayData.isDisplay ?? 1),
        });

        this.applyReadonlyState();
        this.ensureCurrentComponentOption();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        this.ui.error('Không tải được chi tiết');
      },
    });
  }

  goBack(): void {
    if (this.dialogMode) {
      this.cancelled.emit();
      return;
    }

    void this.router.navigate(['/categories']);
  }

  save(): void {
    if (this.readonlyForm) {
      this.ui.warning('Trạng thái hiện tại không cho phép lưu');
      return;
    }

    if (!this.validateForm()) {
      return;
    }

    const payload = this.buildPayload();

    this.saving = true;

    if (this.mode === 'create' || this.mode === 'copy') {
      this.categoryService.create(payload).subscribe({
        next: () => {
          this.saving = false;
          this.ui.success('Lưu thành công');
          this.finishSuccess();
        },
        error: (err) => {
          console.error(err);
          this.saving = false;
          this.ui.error(err?.error?.message || 'Lưu thất bại');
        },
      });

      return;
    }

    if (!this.id) {
      this.saving = false;
      this.ui.error('Không tìm thấy ID bản ghi');
      return;
    }

    this.categoryService.update(this.id, payload).subscribe({
      next: () => {
        this.saving = false;
        this.ui.success('Cập nhật thành công');
        this.finishSuccess();
      },
      error: (err) => {
        console.error(err);
        this.saving = false;
        this.ui.error(err?.error?.message || 'Cập nhật thất bại');
      },
    });
  }

  captureComponentCodeDropdownState(): void {
    this.componentCodeDropdownOpenBeforeTriggerClick = this.componentCodeDropdownOpen;
    this.ignoreComponentCodeDropdownOpenChange = true;
    this.releaseComponentCodeDropdownOpenChange(250);
  }

  toggleComponentCodeDropdown(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();

    if (this.loading || this.saving || this.readonlyForm || this.componentOptionsLoading) {
      this.componentCodeDropdownOpen = false;
      this.releaseComponentCodeDropdownOpenChange();
      return;
    }

    this.ignoreComponentCodeDropdownOpenChange = true;
    this.componentCodeDropdownOpen = !this.componentCodeDropdownOpenBeforeTriggerClick;
    this.releaseComponentCodeDropdownOpenChange(50);
  }

  onComponentCodeDropdownOpenChange(open: boolean): void {
    if (this.loading || this.saving || this.readonlyForm || this.componentOptionsLoading) {
      this.componentCodeDropdownOpen = false;
      return;
    }

    if (this.ignoreComponentCodeDropdownOpenChange) {
      return;
    }

    this.componentCodeDropdownOpen = open;
  }

  selectComponentCode(value: string, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();

    const control = this.form.controls.componentCode;

    control.setValue(value);
    control.markAsDirty();
    control.markAsTouched();
    control.updateValueAndValidity();

    this.componentCodeDropdownOpen = false;
    queueMicrotask(() => {
      this.componentCodeDropdownOpen = false;
    });
    setTimeout(() => {
      this.componentCodeDropdownOpen = false;
    }, 0);
  }

  openSaveAndSubmitConfirm(): void {
    if (this.readonlyForm) {
      this.ui.warning('Trạng thái hiện tại không cho phép gửi duyệt');
      return;
    }

    if (!this.validateForm()) {
      return;
    }

    this.ui
      .confirm({
        title: 'Xác nhận gửi duyệt',
        message: 'Bạn có chắc chắn lưu và gửi duyệt bản ghi này?',
        confirmText: 'Lưu và gửi duyệt',
        cancelText: 'Hủy',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          this.saveAndSubmit();
        }
      });
  }

  saveAndSubmit(): void {
    if (this.readonlyForm) {
      this.ui.warning('Trạng thái hiện tại không cho phép gửi duyệt');
      return;
    }

    if (!this.validateForm()) {
      return;
    }

    const payload = this.buildPayload();

    this.saving = true;

    if (this.mode === 'create' || this.mode === 'copy') {
      this.categoryService.createAndSubmit(payload).subscribe({
        next: () => {
          this.saving = false;
          this.ui.success('Lưu và gửi duyệt thành công');
          this.finishSuccess();
        },
        error: (err) => {
          console.error(err);
          this.saving = false;
          this.ui.error(err?.error?.message || 'Lưu thất bại');
        },
      });

      return;
    }

    if (!this.id) {
      this.saving = false;
      this.ui.error('Không tìm thấy ID bản ghi');
      return;
    }

    this.categoryService.update(this.id, payload).subscribe({
      next: () => {
        this.categoryService.submit(this.id!).subscribe({
          next: () => {
            this.saving = false;
            this.ui.success('Cập nhật và gửi duyệt thành công');
            this.finishSuccess();
          },
          error: (err) => {
            console.error(err);
            this.saving = false;
            this.ui.error(err?.error?.message || 'Gửi duyệt thất bại');
          },
        });
      },
      error: (err) => {
        console.error(err);
        this.saving = false;
        this.ui.error(err?.error?.message || 'Cập nhật thất bại');
      },
    });
  }

  hasError(
    controlName:
      | 'paramName'
      | 'paramValue'
      | 'paramType'
      | 'componentCode'
      | 'effectiveDate'
      | 'endEffectiveDate',
    errorCode: string,
  ): boolean {
    const control = this.control(controlName);

    return control.hasError(errorCode) && control.touched;
  }

  hasFormError(errorCode: string): boolean {
    return (
      this.form.hasError(errorCode) &&
      (this.form.controls.effectiveDate.touched || this.form.controls.endEffectiveDate.touched)
    );
  }

  get title(): string {
    if (this.mode === 'edit') {
      return 'Sửa tham số theo nhóm';
    }

    if (this.mode === 'copy') {
      return 'Sao chép tham số danh mục theo nhóm';
    }

    return 'Thêm mới tham số danh mục theo nhóm';
  }

  private validateForm(): boolean {
    if (this.form.valid) {
      return true;
    }

    this.form.markAllAsTouched();

    if (
      this.hasError('effectiveDate', 'pastDate') ||
      this.hasError('endEffectiveDate', 'pastDate')
    ) {
      this.ui.warning('Ngày hiệu lực hoặc ngày hết hiệu lực không được nhỏ hơn ngày hiện tại');
      return false;
    }

    if (this.form.hasError('endBeforeEffective')) {
      this.ui.warning('Ngày hết hiệu lực phải lớn hơn hoặc bằng ngày hiệu lực');
      return false;
    }

    this.ui.warning('Vui lòng nhập đầy đủ các trường bắt buộc');

    return false;
  }

  private loadComponentOptions(): void {
    this.componentOptionsLoading = true;
    this.componentOptionsLoadFailed = false;
    this.componentCodeDropdownOpen = false;

    this.categoryService.getComponentCodes().subscribe({
      next: (options) => {
        this.componentOptions = options;
        this.componentOptionsLoading = false;
        this.ensureCurrentComponentOption();
      },
      error: (err) => {
        console.error(err);
        this.componentOptions = [];
        this.componentOptionsLoading = false;
        this.componentOptionsLoadFailed = true;
        this.componentCodeDropdownOpen = false;
        this.ui.error(err?.error?.message || 'Không tải được danh sách cấu phần xử lý');
      },
    });
  }

  private ensureCurrentComponentOption(): void {
    const currentValue = this.form.controls.componentCode.value?.trim();

    if (!currentValue || this.componentOptions.includes(currentValue)) {
      return;
    }

    this.componentOptions = [...this.componentOptions, currentValue];
  }

  private releaseComponentCodeDropdownOpenChange(delay = 0): void {
    setTimeout(() => {
      this.ignoreComponentCodeDropdownOpenChange = false;
    }, delay);
  }

  private control(controlName: keyof typeof this.form.controls): AbstractControl {
    return this.form.controls[controlName];
  }

  private finishSuccess(): void {
    if (this.dialogMode) {
      this.completed.emit();
      return;
    }

    void this.router.navigate(['/categories']);
  }

  private applyReadonlyState(): void {
    this.readonlyForm = this.mode === 'edit' && this.isReadonlyByStatus(this.currentStatus);

    if (this.readonlyForm) {
      this.form.disable({ emitEvent: false });
      return;
    }

    this.form.enable({ emitEvent: false });
  }

  private isReadonlyByStatus(status: number | null | undefined): boolean {
    return CategoryStatusPolicy.isReadonly(status);
  }

  private toTuiDay(value: string | null | undefined): TuiDay | null {
    if (!value) {
      return null;
    }

    // Trường hợp backend trả về dạng yyyy-MM-dd hoặc yyyy-MM-ddTHH:mm:ss
    const normalized = value.includes('T') ? value.split('T')[0] : value;
    const parts = normalized.split('-');

    if (parts.length !== 3) {
      return null;
    }

    const year = Number(parts[0]);
    const month = Number(parts[1]);
    const day = Number(parts[2]);

    if (!year || !month || !day) {
      return null;
    }

    return new TuiDay(year, month - 1, day);
  }

  private fromTuiDay(value: TuiDay | null): string | null {
    if (!value) {
      return null;
    }

    const year = value.year;
    const month = String(value.month + 1).padStart(2, '0');
    const day = String(value.day).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  private buildPayload(): Category {
    const raw = this.form.getRawValue();
    const payload = {
      ...raw,
      effectiveDate: this.fromTuiDay(raw.effectiveDate),
      endEffectiveDate: this.fromTuiDay(raw.endEffectiveDate),
    } as Category;

    if (this.mode === 'copy') {
      delete payload.id;
      delete payload.newData;

      return {
        ...payload,
        status: CategoryStatus.New,
        isDisplay: 1,
      };
    }

    return payload;
  }

  private mergeNewDataPreview(data: Category): Category {
    const patch = parseCategoryNewData(data.newData);

    return patch ? { ...data, ...patch } : data;
  }

  private notPastDateValidator(): ValidatorFn {
    return (control: AbstractControl<TuiDay | null>): ValidationErrors | null => {
      const value = control.value;

      if (!value) {
        return null;
      }

      const today = this.todayAsTuiDay();

      return this.compareTuiDay(value, today) < 0 ? { pastDate: true } : null;
    };
  }

  private endDateAfterEffectiveDateValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const effectiveDate = control.get('effectiveDate')?.value as TuiDay | null;
      const endEffectiveDate = control.get('endEffectiveDate')?.value as TuiDay | null;

      if (!effectiveDate || !endEffectiveDate) {
        return null;
      }

      return this.compareTuiDay(endEffectiveDate, effectiveDate) < 0
        ? { endBeforeEffective: true }
        : null;
    };
  }

  private todayAsTuiDay(): TuiDay {
    const now = new Date();

    return new TuiDay(now.getFullYear(), now.getMonth(), now.getDate());
  }

  private compareTuiDay(left: TuiDay, right: TuiDay): number {
    const leftValue = left.year * 10000 + (left.month + 1) * 100 + left.day;
    const rightValue = right.year * 10000 + (right.month + 1) * 100 + right.day;

    return leftValue - rightValue;
  }
}
