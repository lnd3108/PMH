import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject, isDevMode } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TuiButton, TuiDialog } from '@taiga-ui/core';

import { UiFeedbackService } from '../../../core/ui-feedback.service';
import {
  CategoryStatus,
  CategoryStatusPolicy,
  GROUP_CATEGORY_STATUS,
} from '../../../domain/category/category-status';
import { hasMeaningfulCategoryNewData } from '../../../domain/category/category-new-data';
import { Category } from '../../../models/category.models';
import { CategoryPermissionService } from '../../../service/category-permission.service';
import { CategoryService } from '../../../service/category.service';
import { TuiTextarea } from '@taiga-ui/kit';

export type CategoryDetailMode = 'detail' | 'submit' | 'approve' | 'cancel-approve';

type DisplayFieldKey =
  | 'paramName'
  | 'paramValue'
  | 'paramType'
  | 'componentCode'
  | 'effectiveDate'
  | 'endEffectiveDate'
  | 'description';

type DisplayData = Record<DisplayFieldKey, unknown>;

type CompareMode = 'CREATE_OR_DRAFT' | 'UPDATE_WITH_NEW_DATA' | 'VIEW_CURRENT';

type CompareData = {
  mode: CompareMode;
  oldData: DisplayData | null;
  newData: DisplayData;
  changedFields: Set<string>;
  oldEmpty: boolean;
};

type DisplayEntry = {
  key: DisplayFieldKey;
  label: string;
  value: string;
};

type RawCategoryRecord = Category & Record<string, unknown>;

const STATUS = GROUP_CATEGORY_STATUS;

const DISPLAY_FIELDS: Array<{ key: DisplayFieldKey; label: string }> = [
  { key: 'paramName', label: 'Tên thành phần' },
  { key: 'paramValue', label: 'Giá trị thành phần' },
  { key: 'paramType', label: 'Danh mục theo nhóm' },
  { key: 'componentCode', label: 'Cấu phần xử lý' },
  { key: 'effectiveDate', label: 'Ngày hiệu lực' },
  { key: 'endEffectiveDate', label: 'Ngày hết hiệu lực' },
  { key: 'description', label: 'Mô tả' },
];

const NEW_DATA_SOURCE_KEYS = ['newData', 'NEW_DATA', 'new_data'];

const NEW_DATA_FIELD_MAP = new Map<string, DisplayFieldKey>([
  ['paramname', 'paramName'],
  ['PARAM_NAME', 'paramName'],
  ['paramvalue', 'paramValue'],
  ['PARAM_VALUE', 'paramValue'],
  ['paramtype', 'paramType'],
  ['PARAM_TYPE', 'paramType'],
  ['componentcode', 'componentCode'],
  ['COMPONENT_CODE', 'componentCode'],
  ['effectivedate', 'effectiveDate'],
  ['EFFECTIVE_DATE', 'effectiveDate'],
  ['endeffectivedate', 'endEffectiveDate'],
  ['END_EFFECTIVE_DATE', 'endEffectiveDate'],
  ['description', 'description'],
  ['DESCRIPTION', 'description'],
]);

@Component({
  selector: 'app-category-detail-page',
  standalone: true,
  imports: [CommonModule, FormsModule, TuiButton, TuiDialog, TuiTextarea],
  templateUrl: './category-detail-page.html',
  styleUrl: './category-detail-page.css',
})
export class CategoryDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ui = inject(UiFeedbackService);
  private readonly categoryService = inject(CategoryService);
  private readonly permissionService = inject(CategoryPermissionService);

  @Input() mode: CategoryDetailMode = 'submit';
  @Input() id: number | null = null;
  @Input() dialogMode = false;
  @Output() completed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  loading = false;
  actionLoading = false;
  record: Category | null = null;
  compareData: CompareData | null = null;
  showRejectDialog = false;
  rejectReason = '';
  readonly rejectReasonMaxLength = 500;
  rejecting = false;

  ngOnInit(): void {
    if (!this.dialogMode) {
      this.mode = (this.route.snapshot.data['mode'] ?? 'submit') as CategoryDetailMode;
      const idParam = this.route.snapshot.paramMap.get('id');
      this.id = idParam ? Number(idParam) : null;
    }

    if (this.id) {
      this.loadDetail(this.id);
    }
  }

  loadDetail(id: number): void {
    this.loading = true;

    this.categoryService.getById(id).subscribe({
      next: (data) => {
        this.logDetailResponse(data);
        this.record = data;
        this.compareData = this.buildCompareData(data);
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

  openSubmitConfirm(): void {
    if (!this.id) {
      return;
    }

    if (!this.canConfirmSubmit) {
      this.ui.warning('Trạng thái hiện tại không cho phép gửi duyệt');
      return;
    }

    this.ui
      .confirm({
        title: 'Xác nhận gửi duyệt',
        message: 'Bạn có chắc chắn gửi duyệt bản ghi này?',
        confirmText: 'Gửi duyệt',
        cancelText: 'Hủy',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          this.confirmSubmit();
        }
      });
  }

  openApproveConfirm(): void {
    if (!this.id) {
      return;
    }

    if (!this.canConfirmApprove) {
      this.ui.warning('Chỉ phê duyệt bản ghi đang chờ duyệt');
      return;
    }

    this.ui
      .confirm({
        title: 'Xác nhận phê duyệt',
        message: 'Bạn có chắc chắn phê duyệt bản ghi này?',
        confirmText: 'Phê duyệt',
        cancelText: 'Hủy',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          this.confirmApprove();
        }
      });
  }

  openCancelApproveConfirm(): void {
    if (!this.id) {
      return;
    }

    if (!this.canConfirmCancelApprove) {
      this.ui.warning('Chỉ hủy duyệt bản ghi đã duyệt');
      return;
    }

    this.ui
      .confirm({
        title: 'Xác nhận hủy phê duyệt',
        message: 'Bạn có chắc chắn hủy phê duyệt bản ghi này?',
        confirmText: 'Hủy phê duyệt',
        cancelText: 'Hủy',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          this.confirmCancelApprove();
        }
      });
  }

  confirmSubmit(): void {
    if (!this.id) {
      return;
    }

    if (!this.canConfirmSubmit) {
      this.ui.warning('Trạng thái hiện tại không cho phép gửi duyệt');
      return;
    }

    this.actionLoading = true;

    this.categoryService.submit(this.id).subscribe({
      next: () => {
        this.actionLoading = false;
        this.ui.success('Gửi duyệt thành công');
        this.finishSuccess();
      },
      error: (err) => {
        console.error(err);
        this.actionLoading = false;
        this.ui.error(err?.error?.message || 'Gửi duyệt thất bại');
      },
    });
  }

  confirmApprove(): void {
    if (!this.id) {
      return;
    }

    if (!this.canConfirmApprove) {
      this.ui.warning('Chỉ phê duyệt bản ghi đang chờ duyệt');
      return;
    }

    this.actionLoading = true;

    this.categoryService.approve(this.id).subscribe({
      next: () => {
        this.actionLoading = false;
        this.ui.success('Phê duyệt thành công');
        this.finishSuccess();
      },
      error: (err) => {
        console.error('Approve error:', err);
        this.actionLoading = false;
        this.ui.error(err?.error?.message || 'Phê duyệt thất bại');
      },
    });
  }

  openRejectDialog(): void {
    if (!this.canConfirmReject) {
      this.ui.warning('Chỉ từ chối bản ghi đang chờ duyệt');
      return;
    }

    this.rejectReason = '';
    this.showRejectDialog = true;
  }

  closeRejectDialog(): void {
    if (this.rejecting) {
      return;
    }

    this.showRejectDialog = false;
    this.rejectReason = '';
  }

  onRejectDialogOpenChange(open: boolean): void {
    this.showRejectDialog = open;

    if (!open && !this.rejecting) {
      this.rejectReason = '';
    }
  }

  onRejectReasonInput(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    this.rejectReason = textarea.value.slice(0, this.rejectReasonMaxLength);
  }

  submitReject(): void {
    if (!this.id) {
      return;
    }

    if (!this.canConfirmReject) {
      this.ui.warning('Chỉ từ chối bản ghi đang chờ duyệt');
      return;
    }

    const reason = this.rejectReason.trim();
    if (!reason) {
      this.ui.warning('Vui lòng nhập lý do từ chối');
      return;
    }

    this.ui
      .confirm({
        title: 'Xác nhận từ chối',
        message: 'Bạn có chắc chắn từ chối bản ghi này?',
        confirmText: 'Từ chối',
        cancelText: 'Hủy',
        confirmAppearance: 'negative',
      })
      .subscribe((confirmed) => {
        if (!confirmed || !this.id) {
          return;
        }

        this.rejecting = true;

        this.categoryService.reject(this.id, { reason }).subscribe({
          next: () => {
            this.rejecting = false;
            this.showRejectDialog = false;
            this.rejectReason = '';
            this.ui.success('Từ chối thành công');
            this.finishSuccess();
          },
          error: (err) => {
            this.rejecting = false;
            console.error(err);
            this.ui.error(err?.error?.message || 'Từ chối thất bại');
          },
        });
      });
  }

  confirmCancelApprove(): void {
    if (!this.id) {
      return;
    }

    if (!this.canConfirmCancelApprove) {
      this.ui.warning('Chỉ hủy duyệt bản ghi đã duyệt');
      return;
    }

    this.actionLoading = true;

    this.categoryService.cancelApprove(this.id).subscribe({
      next: () => {
        this.actionLoading = false;
        this.ui.success('Hủy duyệt thành công');
        this.finishSuccess();
      },
      error: (err) => {
        console.error(err);
        this.actionLoading = false;
        this.ui.error(err?.error?.message || 'Hủy duyệt thất bại');
      },
    });
  }

  deleteRecord(): void {
    if (!this.id) {
      return;
    }

    if (!this.canDeleteRecord) {
      this.ui.warning('Trạng thái hiện tại hoặc isDisplay không cho phép xóa');
      return;
    }

    this.ui
      .confirm({
        title: 'Xác nhận xóa',
        message: 'Bản ghi này sẽ bị xóa khỏi hệ thống.',
        confirmText: 'Xóa',
        cancelText: 'Hủy',
        confirmAppearance: 'negative',
      })
      .subscribe((confirmed) => {
        if (!confirmed || !this.id) {
          return;
        }

        this.actionLoading = true;

        this.categoryService.delete(this.id).subscribe({
          next: () => {
            this.actionLoading = false;
            this.ui.success('Xóa thành công');
            this.finishSuccess();
          },
          error: (err) => {
            console.error(err);
            this.actionLoading = false;
            this.ui.error(err?.error?.message || 'Xóa thất bại');
          },
        });
      });
  }

  get canConfirmSubmit(): boolean {
    return this.canSubmit(this.record);
  }

  get canConfirmApprove(): boolean {
    return this.canApprove(this.record);
  }

  get canConfirmReject(): boolean {
    return this.canReject(this.record);
  }

  get canConfirmCancelApprove(): boolean {
    return this.canCancelApprove(this.record);
  }

  get canDeleteRecord(): boolean {
    return this.canDelete(this.record);
  }

  get showSubmitActions(): boolean {
    return this.canConfirmSubmit || this.canDeleteRecord;
  }

  get showApproveActions(): boolean {
    return this.canConfirmApprove || this.canConfirmReject;
  }

  get dialogTitle(): string {
    return 'Xem chi tiết';
  }

  get statusLabel(): string {
    if (this.record?.status != null) {
      return `${this.record.status} - ${CategoryStatusPolicy.label(this.record.status)}`;
    }

    return 'Chi tiết bản ghi';
  }

  get badgeClass(): string {
    return CategoryStatusPolicy.badgeClass(this.record?.status);
  }

  get oldDataEntries(): DisplayEntry[] {
    return DISPLAY_FIELDS.map(({ key, label }) => ({
      key,
      label,
      value: this.compareData?.oldEmpty ? '' : this.formatValue(this.compareData?.oldData?.[key]),
    }));
  }

  get newDataEntries(): DisplayEntry[] {
    return DISPLAY_FIELDS.map(({ key, label }) => ({
      key,
      label,
      value: this.formatValue(this.compareData?.newData?.[key]),
    }));
  }

  isChanged(field: string): boolean {
    return this.compareData?.changedFields?.has(field) ?? false;
  }

  private canSubmit(record: Category | null): boolean {
    return (
      new Set<number>([STATUS.DRAFT, STATUS.REJECTED, STATUS.CANCEL_APPROVED]).has(
        Number(record?.status),
      ) && this.permissionService.can('submit')
    );
  }

  private canApprove(record: Category | null): boolean {
    return Number(record?.status) === STATUS.PENDING && this.permissionService.can('approve');
  }

  private canReject(record: Category | null): boolean {
    return Number(record?.status) === STATUS.PENDING && this.permissionService.can('approve');
  }

  private canDelete(record: Category | null): boolean {
    if (!record) {
      return false;
    }

    const status = Number(record.status);

    if (status === STATUS.DRAFT) {
      return this.permissionService.can('delete');
    }

    return (
      this.permissionService.can('delete') &&
      CategoryStatusPolicy.canDelete(
        record.status,
        record.isDisplay,
        hasMeaningfulCategoryNewData(this.getRawNewData(record)),
      )
    );
  }

  private canCancelApprove(record: Category | null): boolean {
    return (
      Number(record?.status) === STATUS.APPROVED && this.permissionService.can('cancelApprove')
    );
  }

  private finishSuccess(): void {
    if (this.dialogMode) {
      this.completed.emit();
      return;
    }

    void this.router.navigate(['/categories']);
  }

  private parseNewData(value: unknown): Partial<DisplayData> {
    if (value === null || value === undefined) {
      return {};
    }

    if (typeof value === 'string' && !value.trim()) {
      return {};
    }

    if (typeof value === 'object') {
      return this.normalizeNewDataFields(value as Record<string, unknown>);
    }

    try {
      const parsed = JSON.parse(String(value)) as unknown;

      return parsed && typeof parsed === 'object'
        ? this.normalizeNewDataFields(parsed as Record<string, unknown>)
        : {};
    } catch {
      this.ui.warning('Dữ liệu thay đổi không đúng định dạng, đang hiển thị dữ liệu hiện tại');
      return {};
    }
  }

  private normalizeNewDataFields(value: Record<string, unknown>): Partial<DisplayData> {
    const normalized: Partial<DisplayData> = {};

    Object.entries(value).forEach(([key, fieldValue]) => {
      const displayKey = this.toDisplayFieldKey(key);

      if (displayKey) {
        normalized[displayKey] = fieldValue;
      }
    });

    return normalized;
  }

  get detailStatusClass(): string {
    return `detail-status-pill--${this.statusTone}`;
  }

  private get statusTone():
    | 'draft'
    | 'pending'
    | 'approved'
    | 'rejected'
    | 'cancel-approved'
    | 'inactive' {
    switch (Number(this.record?.status)) {
      case CategoryStatus.New:
        return 'draft';

      case CategoryStatus.PendingApproval:
        return 'pending';

      case CategoryStatus.Approved:
        return 'approved';

      case CategoryStatus.Rejected:
        return 'rejected';

      case CategoryStatus.CancelApproved:
        return 'cancel-approved';

      default:
        return 'inactive';
    }
  }

  private toDisplayFieldKey(key: string): DisplayFieldKey | null {
    const normalizedKey = key.replace(/[_\-\s]/g, '').toLowerCase();

    return NEW_DATA_FIELD_MAP.get(key) ?? NEW_DATA_FIELD_MAP.get(normalizedKey) ?? null;
  }

  private getRawNewData(record: Category): unknown {
    const rawRecord = record as RawCategoryRecord;

    for (const key of NEW_DATA_SOURCE_KEYS) {
      if (rawRecord[key] !== undefined && rawRecord[key] !== null) {
        return rawRecord[key];
      }
    }

    return undefined;
  }

  private logDetailResponse(record: Category): void {
    if (!isDevMode()) {
      return;
    }

    const rawNewData = this.getRawNewData(record);
    const parsedNewData = this.parseNewData(rawNewData);
    const hasMeaningfulNewData = hasMeaningfulCategoryNewData(rawNewData);
    const canDelete = this.canDelete(record);

    console.debug('[CategoryDetail] API detail response', {
      id: record.id,
      status: record.status,
      isDisplay: record.isDisplay,
      newDataType: typeof rawNewData,
      newDataRaw: rawNewData,
      parsedNewData,
      hasMeaningfulNewData,
      canDeletePermission: this.permissionService.can('delete'),
      canDelete,
      paramName: record.paramName,
      paramValue: record.paramValue,
      paramType: record.paramType,
      componentCode: record.componentCode,
      effectiveDate: record.effectiveDate,
      endEffectiveDate: record.endEffectiveDate,
      description: record.description,
    });
  }

  private normalizeValue(value: unknown, field?: DisplayFieldKey): string {
    if (value === null || value === undefined) {
      return '';
    }

    const normalized = String(value).trim();

    if (field === 'effectiveDate' || field === 'endEffectiveDate') {
      return this.normalizeDateValue(normalized);
    }

    return normalized;
  }

  private getChangedFields(oldData: DisplayData, newData: DisplayData): Set<string> {
    const changed = new Set<string>();

    Object.keys(newData).forEach((key) => {
      const field = key as DisplayFieldKey;

      if (
        this.normalizeValue(oldData[field], field) !== this.normalizeValue(newData[field], field)
      ) {
        changed.add(key);
      }
    });

    return changed;
  }

  private mapRecordToDisplayData(record: Category): DisplayData {
    return {
      paramName: record.paramName,
      paramValue: record.paramValue,
      paramType: record.paramType,
      componentCode: record.componentCode,
      effectiveDate: record.effectiveDate,
      endEffectiveDate: record.endEffectiveDate,
      description: record.description,
    };
  }

  private buildCompareData(record: Category): CompareData {
    const status = Number(record.status);
    const baseData = this.mapRecordToDisplayData(record);
    const patchData = this.parseNewData(this.getRawNewData(record));
    const hasPatch = Object.keys(patchData).length > 0;

    if (hasPatch) {
      const newData = {
        ...baseData,
        ...patchData,
      } as DisplayData;

      return {
        mode: 'UPDATE_WITH_NEW_DATA',
        oldData: baseData,
        newData,
        changedFields: this.getChangedFields(baseData, newData),
        oldEmpty: false,
      };
    }

    if (status === STATUS.DRAFT || status === STATUS.PENDING) {
      return {
        mode: 'CREATE_OR_DRAFT',
        oldData: null,
        newData: baseData,
        changedFields: new Set<string>(),
        oldEmpty: true,
      };
    }

    return {
      mode: 'VIEW_CURRENT',
      oldData: null,
      newData: baseData,
      changedFields: new Set<string>(),
      oldEmpty: true,
    };
  }

  private formatValue(value: unknown): string {
    if (value === null || value === undefined || value === '') {
      return '-';
    }

    return String(value);
  }

  private normalizeDateValue(value: string): string {
    if (!value) {
      return '';
    }

    const dateOnly = value.includes('T') ? value.split('T')[0] : value.split(' ')[0];
    const match = dateOnly.match(/^(\d{4})-(\d{2})-(\d{2})$/);

    return match ? `${match[1]}-${match[2]}-${match[3]}` : value;
  }
}
