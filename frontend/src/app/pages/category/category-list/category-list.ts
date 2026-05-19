import { CommonModule } from '@angular/common';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TuiButton, TuiDialog } from '@taiga-ui/core';

import { UiFeedbackService } from '../../../core/ui-feedback.service';
import { CategoryEntity } from '../../../domain/category/category.entity';
import { CategoryFilterForm } from '../../../domain/category/category-filter';
import { Category } from '../../../models/category.models';
import {
  CategoryPermissionService,
  CategoryAction,
} from '../../../service/category-permission.service';
import {
  CategoryBatchActionResponse,
  CategoryExportRequest,
  CategoryImportExcelResponse,
  CategoryService,
} from '../../../service/category.service';
import { CategoryFiltersComponent } from '../components/category-filters/category-filters';
import { CategoryTableComponent } from '../components/category-table/category-table';
import {
  CategoryDetailMode,
  CategoryDetailPage,
} from '../category-detail-page/category-detail-page';
import { CategoryFormMode, CategoryFormPage } from '../category-form-page/category-form-page';
import { CategoryListFacade } from './category-list.facade';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TuiButton,
    TuiDialog,
    CategoryFiltersComponent,
    CategoryTableComponent,
    CategoryFormPage,
    CategoryDetailPage,
  ],
  templateUrl: './category-list.html',
  styleUrl: './category-list.css',
  providers: [CategoryListFacade],
})
export class CategoryList implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly permissionService = inject(CategoryPermissionService);
  private readonly ui = inject(UiFeedbackService);

  readonly facade = inject(CategoryListFacade);

  @ViewChild('importFileInput')
  private importFileInput?: ElementRef<HTMLInputElement>;

  exportingExcel = false;
  importingExcel = false;
  downloadingTemplate = false;
  importResult: CategoryImportExcelResponse | null = null;
  formDialogOpen = false;
  formDialogMode: CategoryFormMode = 'create';
  formDialogId: number | null = null;
  detailDialogOpen = false;
  detailDialogMode: CategoryDetailMode = 'detail';
  detailDialogId: number | null = null;

  ngOnInit(): void {
    this.facade.loadCategories();
  }

  onSearch(filters: CategoryFilterForm): void {
    this.facade.applySearch(filters);
  }

  onResetFilters(): void {
    this.facade.resetFilters();
  }

  onPageChange(newPage: number): void {
    this.facade.changePage(newPage);
  }

  onSizeChange(newSize: number): void {
    this.facade.changeSize(newSize);
  }

  onToggleSelectAll(checked: boolean): void {
    this.facade.toggleSelectAll(checked);
  }

  onToggleSelectItem(event: { item: Category; checked: boolean }): void {
    this.facade.toggleSelectItem(event.item, event.checked);
  }

  bulkSubmitSelected(): void {
    if (!this.ensurePermission('submit')) return;

    if (!this.facade.canBulkSubmit) {
      this.ui.warning('Chỉ được gửi duyệt nhiều bản ghi khi các bản ghi cùng trạng thái hợp lệ.');
      return;
    }

    const ids = this.getSelectedIds();

    this.executeBatchAction(
      ids,
      () => this.categoryService.submitBatch(ids),
      'Xác nhận gửi duyệt',
      'Gửi duyệt',
    );
  }

  bulkApproveSelected(): void {
    if (!this.ensurePermission('approve')) return;

    if (!this.facade.canBulkApprove) {
      this.ui.warning(
        'Chỉ được phê duyệt nhiều bản ghi khi tất cả đang ở trạng thái chờ phê duyệt.',
      );
      return;
    }

    const ids = this.getSelectedIds();

    this.executeBatchAction(
      ids,
      () => this.categoryService.approveBatch(ids),
      'Xác nhận phê duyệt',
      'Phê duyệt',
    );
  }

  bulkCancelApproveSelected(): void {
    if (!this.ensurePermission('cancelApprove')) return;

    if (!this.facade.canBulkCancelApprove) {
      this.ui.warning(
        'Chỉ được hủy duyệt nhiều bản ghi khi tất cả đang ở trạng thái đã phê duyệt.',
      );
      return;
    }

    const ids = this.getSelectedIds();

    this.executeBatchAction(
      ids,
      () => this.categoryService.cancelApproveBatch(ids),
      'Xác nhận hủy duyệt',
      'Hủy duyệt',
    );
  }

  bulkDeleteSelected(): void {
    if (!this.ensurePermission('delete')) return;

    if (!this.facade.canBulkDelete) {
      this.ui.warning('Chỉ được xóa nhiều bản ghi hợp lệ, không bao gồm bản ghi đã duyệt.');
      return;
    }

    const ids = this.getSelectedIds();

    this.ui
      .confirm({
        title: 'Xác nhận xóa hàng loạt',
        message: `Bản ghi đã chọn sẽ bị xóa khỏi hệ thống (${ids.length} bản ghi).`,
        confirmText: 'Xóa',
        cancelText: 'Hủy',
        confirmAppearance: 'negative',
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;

        this.facade.loading = true;

        this.categoryService.deleteBatch(ids).subscribe({
          next: (response: CategoryBatchActionResponse) => {
            this.facade.loading = false;
            this.facade.loadCategories();

            if ((response.failedCount ?? 0) > 0) {
              const firstError = response.failed?.[0]?.message || 'Xóa có bản ghi thất bại';

              this.ui.warning(
                `Xóa thành công ${response.successCount}/${response.totalRequested}. ${firstError}`,
              );
              return;
            }

            this.ui.success(`Xóa thành công ${response.successCount} bản ghi`);
          },
          error: (err) => {
            this.facade.loading = false;
            console.error(err);
            this.ui.error(err?.error?.message || 'Xóa hàng loạt thất bại');
          },
        });
      });
  }

  goCreate(): void {
    if (!this.ensurePermission('create')) return;

    this.openFormDialog('create');
  }

  goEdit(item: Category): void {
    if (!this.ensurePermission('edit') || !item.id) return;

    if (!CategoryEntity.fromModel(item).canEdit()) {
      this.ui.warning('Trạng thái hiện tại không cho phép sửa');
      return;
    }

    this.openFormDialog('edit', item.id);
  }

  goCopy(item: Category): void {
    if (!this.ensurePermission('copy') || !item.id) return;

    this.openFormDialog('copy', item.id);
  }

  goSubmit(item: Category): void {
    if (!this.ensurePermission('submit') || !item.id) return;

    if (!CategoryEntity.fromModel(item).canSubmit()) {
      this.ui.warning('Trạng thái hiện tại không cho phép gửi duyệt');
      return;
    }

    this.openDetailDialog('submit', item.id);
  }

  goApprove(item: Category): void {
    if (!this.ensurePermission('approve') || !item.id) return;

    if (!CategoryEntity.fromModel(item).canApprove()) {
      this.ui.warning('Chỉ phê duyệt bản ghi đang chờ duyệt');
      return;
    }

    this.openDetailDialog('approve', item.id);
  }

  canReject(item: Category): boolean {
    return CategoryEntity.fromModel(item).canReject();
  }

  goCancelApprove(item: Category): void {
    if (!this.ensurePermission('cancelApprove')) return;

    const id = item.id;

    if (id == null) {
      this.ui.error('Không tìm thấy ID bản ghi');
      return;
    }

    if (!CategoryEntity.fromModel(item).canCancelApprove()) {
      this.ui.warning('Chỉ hủy duyệt bản ghi đã duyệt');
      return;
    }

    this.openDetailDialog('cancel-approve', id);
  }

  deleteCategory(item: Category): void {
    if (!this.ensurePermission('delete')) return;

    const id = item.id;

    if (id == null) {
      this.ui.error('Không tìm thấy ID bản ghi');
      return;
    }

    if (!CategoryEntity.fromModel(item).canDelete()) {
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
        if (!confirmed) return;

        this.categoryService.delete(id).subscribe({
          next: () => {
            this.ui.success('Xóa thành công');
            this.facade.loadCategories();
          },
          error: (err) => {
            console.error(err);
            this.ui.error(err?.error?.message || 'Xóa thất bại');
          },
        });
      });
  }

  goDetail(item: Category): void {
    if (!item.id) return;

    this.openDetailDialog('detail', item.id);
  }

  closeFormDialog(): void {
    this.formDialogOpen = false;
    this.formDialogId = null;
  }

  closeDetailDialog(): void {
    this.detailDialogOpen = false;
    this.detailDialogId = null;
  }

  onDialogCompleted(): void {
    this.closeFormDialog();
    this.closeDetailDialog();
    this.facade.refresh();
  }

  get formDialogTitle(): string {
    if (this.formDialogMode === 'edit') {
      return 'Sửa tham số theo nhóm';
    }

    if (this.formDialogMode === 'copy') {
      return 'Sao chép tham số danh mục theo nhóm';
    }

    return 'Thêm mới tham số danh mục theo nhóm';
  }

  get detailDialogTitle(): string {
    return 'Xem chi tiết';
  }

  private openFormDialog(mode: CategoryFormMode, id: number | null = null): void {
    this.formDialogMode = mode;
    this.formDialogId = id;
    this.formDialogOpen = true;
  }

  private openDetailDialog(mode: CategoryDetailMode, id: number): void {
    this.detailDialogMode = mode;
    this.detailDialogId = id;
    this.detailDialogOpen = true;
  }

  exportExcel(): void {
    if (!this.ensurePermission('export')) return;

    if (this.exportingExcel) return;

    this.exportingExcel = true;

    this.categoryService.exportExcel(this.buildCurrentExportRequest()).subscribe({
      next: (blob: Blob) => {
        void this.handleExportBlob(blob);
      },
      error: (err) => {
        void this.handleExportError(err);
      },
    });
  }

  downloadTemplateCategory(): void {
    if (!this.ensurePermission('import')) return;

    if (this.downloadingTemplate) return;

    this.downloadingTemplate = true;

    this.categoryService.downloadTemplateCategory().subscribe({
      next: (response: HttpResponse<Blob>) => {
        void this.handleTemplateResponse(response);
      },
      error: (err) => {
        void this.handleTemplateError(err);
      },
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (file && !this.isExcelFile(file)) {
      this.ui.warning('Chỉ chấp nhận file .xls hoặc .xlsx');
      this.clearImportFile();
      return;
    }

    this.importResult = null;
    this.facade.setSelectedFile(file);
  }

  importExcel(): void {
    if (!this.ensurePermission('import')) return;

    if (this.importingExcel) return;

    if (!this.facade.selectedFile) {
      this.ui.warning('Chọn file trước');
      return;
    }

    if (!this.isExcelFile(this.facade.selectedFile)) {
      this.ui.warning('Chỉ chấp nhận file .xls hoặc .xlsx');
      this.clearImportFile();
      return;
    }

    this.importingExcel = true;
    this.importResult = null;

    this.categoryService.importExcel(this.facade.selectedFile).subscribe({
      next: (response) => {
        this.importingExcel = false;

        const result = this.normalizeImportResult(response);

        this.importResult = result;
        this.showImportResult(result);
        this.clearImportFile();

        if ((result.successRows ?? 0) > 0) {
          this.facade.refresh();
        }
      },
      error: (err) => {
        this.importingExcel = false;
        void this.handleImportError(err);
      },
    });
  }

  getImportFailedRows(result: Partial<CategoryImportExcelResponse> | null | undefined): number {
    return Number(result?.failedRows ?? result?.errorRows ?? 0);
  }

  isImportPartial(result: Partial<CategoryImportExcelResponse> | null | undefined): boolean {
    return this.getImportFailedRows(result) > 0 && Number(result?.successRows ?? 0) > 0;
  }

  isImportFailed(result: Partial<CategoryImportExcelResponse> | null | undefined): boolean {
    return this.getImportFailedRows(result) > 0 && Number(result?.successRows ?? 0) === 0;
  }

  private getSelectedIds(): number[] {
    return this.facade.selectedItems
      .map((item) => item.id)
      .filter((id): id is number => id != null);
  }

  private buildCurrentExportRequest(): CategoryExportRequest {
    return {
      ...this.facade.currentFilter.toSearchRequest(),
      sortBy: 'id',
      sortDir: 'desc',
    };
  }

  private async handleExportBlob(blob: Blob): Promise<void> {
    try {
      if (this.isJsonBlob(blob)) {
        const payload = await this.readBlobPayload(blob);
        this.ui.error(this.formatErrorPayload(payload, 'Xuất Excel thất bại'));
        return;
      }

      if (!blob.size) {
        this.ui.error('File Excel trả về rỗng');
        return;
      }

      const excelBlob = new Blob([blob], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });

      const url = window.URL.createObjectURL(excelBlob);
      const link = document.createElement('a');

      link.href = url;
      link.setAttribute('download', `group_category_export_${Date.now()}.xlsx`);

      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      window.URL.revokeObjectURL(url);

      this.ui.success('Xuất Excel thành công');
    } finally {
      this.exportingExcel = false;
    }
  }

  private async handleExportError(err: unknown): Promise<void> {
    console.error('Export Excel error:', err);

    try {
      const payload = await this.extractErrorPayload(err);
      this.ui.error(this.formatErrorPayload(payload, 'Xuất Excel thất bại'));
    } finally {
      this.exportingExcel = false;
    }
  }

  private async handleTemplateResponse(response: HttpResponse<Blob>): Promise<void> {
    const blob = response.body;
    const contentType = response.headers.get('content-type') ?? '';
    const contentDisposition = response.headers.get('content-disposition') ?? '';
    const blobSize = blob?.size ?? 0;

    console.debug('[CategoryTemplate] download response', {
      status: response.status,
      contentType,
      contentDisposition,
      blobSize,
    });

    try {
      if (!blob) {
        this.ui.error('Template category không có dữ liệu trả về');
        return;
      }

      if (this.isJsonBlob(blob)) {
        const payload = await this.readBlobPayload(blob);
        this.ui.error(this.formatErrorPayload(payload, 'Không thể tải template category'));
        return;
      }

      if (!blob.size) {
        this.ui.error('Template category trả về rỗng');
        return;
      }

      const templateBlob = new Blob([blob], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = window.URL.createObjectURL(templateBlob);
      const link = document.createElement('a');

      link.href = url;
      link.setAttribute('download', 'template_category.xlsx');

      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      window.URL.revokeObjectURL(url);

      this.ui.success('Tải template thành công');
    } finally {
      this.downloadingTemplate = false;
    }
  }

  private async handleTemplateError(err: unknown): Promise<void> {
    console.error('Download template error:', err);

    try {
      if (err instanceof HttpErrorResponse) {
        console.debug('[CategoryTemplate] download error response', {
          status: err.status,
          contentType: err.headers.get('content-type'),
          contentDisposition: err.headers.get('content-disposition'),
          blobSize: err.error instanceof Blob ? err.error.size : undefined,
        });
      }

      const payload = await this.extractErrorPayload(err);
      const fallback =
        err instanceof HttpErrorResponse && err.status === 403
          ? 'Không có quyền tải template category'
          : 'Không thể tải template category';

      this.ui.error(this.formatErrorPayload(payload, fallback));
    } finally {
      this.downloadingTemplate = false;
    }
  }

  private async handleImportError(err: unknown): Promise<void> {
    console.error('Import Excel error:', err);

    const payload = await this.extractErrorPayload(err);

    if (this.isImportResultPayload(payload)) {
      const result = this.normalizeImportResult(payload);

      this.importResult = result;
      this.showImportResult(result);

      if ((result.successRows ?? 0) > 0) {
        this.clearImportFile();
        this.facade.refresh();
      }

      return;
    }

    this.ui.error(this.formatErrorPayload(payload, 'Import thất bại'));
  }

  private isExcelFile(file: File): boolean {
    return /\.(xls|xlsx)$/i.test(file.name);
  }

  private clearImportFile(): void {
    this.facade.clearSelectedFile();

    if (this.importFileInput?.nativeElement) {
      this.importFileInput.nativeElement.value = '';
    }
  }

  private normalizeImportResult(
    response: Partial<CategoryImportExcelResponse> | null | undefined,
  ): CategoryImportExcelResponse {
    return {
      totalRows: Number(response?.totalRows ?? 0),
      successRows: Number(response?.successRows ?? 0),
      failedRows: Number(response?.failedRows ?? response?.errorRows ?? 0),
      errors: Array.isArray(response?.errors) ? response.errors : [],
    };
  }

  private showImportResult(result: CategoryImportExcelResponse): void {
    const failedRows = this.getImportFailedRows(result);
    const successText = `${result.successRows}/${result.totalRows} dòng`;

    const firstError = result.errors?.[0]?.message
      ? ` Dòng ${result.errors[0].rowNumber}: ${result.errors[0].message}`
      : '';

    if (failedRows > 0 && result.successRows > 0) {
      this.ui.warning(
        `Thành công ${successText}, lỗi ${failedRows}.${firstError}`,
        'Import hoàn tất một phần',
      );
      return;
    }

    if (failedRows > 0) {
      this.ui.error(
        `Không import được dòng nào. Lỗi ${failedRows}.${firstError}`,
        'Import thất bại',
      );
      return;
    }

    this.ui.success(`Đã import ${successText}`, 'Import thành công');
  }

  private isJsonBlob(blob: Blob): boolean {
    return blob.type.toLowerCase().includes('json');
  }

  private async extractErrorPayload(err: unknown): Promise<unknown> {
    if (err instanceof HttpErrorResponse) {
      if (err.error instanceof Blob) {
        return this.readBlobPayload(err.error);
      }

      return err.error ?? err.message;
    }

    return err;
  }

  private async readBlobPayload(blob: Blob): Promise<unknown> {
    const text = await blob.text();

    if (!text.trim()) {
      return null;
    }

    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }

  private formatErrorPayload(payload: unknown, fallback: string): string {
    if (!payload) {
      return fallback;
    }

    if (typeof payload === 'string') {
      return payload;
    }

    if (typeof payload === 'object') {
      const value = payload as {
        message?: unknown;
        error?: unknown;
        errors?: Array<{ rowNumber?: number; message?: string }>;
      };

      if (typeof value.message === 'string') {
        return value.message;
      }

      if (Array.isArray(value.errors) && value.errors.length > 0) {
        const firstError = value.errors[0];

        return firstError.rowNumber
          ? `Dòng ${firstError.rowNumber}: ${firstError.message ?? fallback}`
          : (firstError.message ?? fallback);
      }

      if (typeof value.error === 'string') {
        return value.error;
      }
    }

    return fallback;
  }

  private isImportResultPayload(payload: unknown): payload is Partial<CategoryImportExcelResponse> {
    return (
      !!payload &&
      typeof payload === 'object' &&
      ('totalRows' in payload ||
        'successRows' in payload ||
        'failedRows' in payload ||
        'errorRows' in payload)
    );
  }

  private executeBatchAction(
    ids: number[],
    requestFactory: () => ReturnType<CategoryService['submitBatch']>,
    title: string,
    actionLabel: string,
  ): void {
    this.ui
      .confirm({
        title,
        message: `Bạn đang thao tác ${ids.length} bản ghi.`,
        confirmText: 'Xác nhận',
        cancelText: 'Hủy',
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;

        this.facade.loading = true;

        requestFactory().subscribe({
          next: (response: CategoryBatchActionResponse) => {
            this.facade.loading = false;
            this.facade.refresh();

            if ((response.failedCount ?? 0) > 0) {
              const firstError =
                response.failed?.[0]?.message || `${actionLabel} có bản ghi thất bại`;

              this.ui.warning(
                `${actionLabel} thành công ${response.successCount}/${response.totalRequested}. ${firstError}`,
              );
              return;
            }

            this.ui.success(`${actionLabel} thành công ${response.successCount} bản ghi`);
          },
          error: (err) => {
            this.facade.loading = false;
            console.error(err);
            this.ui.error(err?.error?.message || `${actionLabel} thất bại`);
          },
        });
      });
  }

  private ensurePermission(action: CategoryAction): boolean {
    if (this.permissionService.can(action)) {
      return true;
    }

    const actionLabel = this.permissionService.getActionLabel(action);

    console.error(`[Category] Không đủ quyền để ${actionLabel}.`, {
      action,
      authorities: this.permissionService.getGrantedAuthorities(),
    });

    this.ui.error(`Không đủ quyền để ${actionLabel}`);

    return false;
  }
}
