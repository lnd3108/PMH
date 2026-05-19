import { Injectable, inject } from '@angular/core';

import { UiFeedbackService } from '../../../core/ui-feedback.service';
import { CategoryEntity } from '../../../domain/category/category.entity';
import { CategoryFilter, CategoryFilterForm } from '../../../domain/category/category-filter';
import { CategorySelection } from '../../../domain/category/category-selection';
import { Category } from '../../../models/category.models';
import {
  CategoryBatchActionResponse,
  CategoryPageResponse,
  CategoryService,
} from '../../../service/category.service';

@Injectable()
export class CategoryListFacade {
  private readonly categoryService = inject(CategoryService);
  private readonly ui = inject(UiFeedbackService);

  private selection = CategorySelection.empty();

  categories: Category[] = [];
  selectedIds = new Set<number>();

  loading = false;
  error = '';

  readonly statusOptions: { value: string; label: string }[] = [
    { value: '', label: 'Tất cả' },
    { value: '1', label: 'Tạo mới' },
    { value: '3', label: 'Chờ phê duyệt' },
    { value: '4', label: 'Đã phê duyệt' },
    { value: '5', label: 'Từ chối' },
    { value: '7', label: 'Hủy duyệt' },
  ];

  readonly activeOptions: { value: string; label: string }[] = [
    { value: '', label: 'Tất cả' },
    { value: '1', label: 'Hoạt động' },
    { value: '0', label: 'Không hoạt động' },
  ];

  currentFilter = CategoryFilter.empty();
  currentFilters: CategoryFilterForm = this.currentFilter.toForm();

  page = 0;
  size = 20;
  totalElements = 0;
  totalPages = 0;
  isFiltering = false;

  selectedFile: File | null = null;

  get selectedItems(): Category[] {
    return this.selection.selectedItems(this.categories);
  }

  get selectedCount(): number {
    return this.selectedItems.length;
  }

  get selectedStatuses(): number[] {
    return [
      ...new Set(
        this.selectedItems
          .map((item) => item.status)
          .filter((value): value is number => value != null),
      ),
    ];
  }

  get hasUniformSelectedStatus(): boolean {
    return this.selectedStatuses.length === 1;
  }

  get allSelectedOnPage(): boolean {
    return this.selection.allSelectedOnPage(this.categories);
  }

  get someSelectedOnPage(): boolean {
    return this.selection.someSelectedOnPage(this.categories);
  }

  get canBulkSubmit(): boolean {
    return (
      this.selectedCount > 0 &&
      this.hasUniformSelectedStatus &&
      this.selectedItems.every((item) => CategoryEntity.fromModel(item).canSubmit())
    );
  }

  get canBulkApprove(): boolean {
    return (
      this.selectedCount > 0 &&
      this.hasUniformSelectedStatus &&
      this.selectedItems.every((item) => CategoryEntity.fromModel(item).canApprove())
    );
  }

  get canBulkCancelApprove(): boolean {
    return (
      this.selectedCount > 0 &&
      this.hasUniformSelectedStatus &&
      this.selectedItems.every((item) => CategoryEntity.fromModel(item).canCancelApprove())
    );
  }

  get canBulkDelete(): boolean {
    return (
      this.selectedCount > 0 &&
      this.selectedItems.every((item) => CategoryEntity.fromModel(item).canDelete())
    );
  }

  loadCategories(): void {
    this.loading = true;
    this.error = '';

    this.categoryService.getAll(this.page, this.size).subscribe({
      next: (response) => {
        this.applyPageResponse(response);
        this.loading = false;
      },
      error: (err) => {
        console.error('Lỗi khi lấy danh sách category:', err);
        this.error = 'Không tải được danh sách category';
        this.loading = false;
        this.ui.error('Không tải được danh sách category');
      },
    });
  }

  applySearch(filters: CategoryFilterForm): void {
    this.currentFilter = CategoryFilter.fromForm(filters);
    this.currentFilters = this.currentFilter.toForm();
    this.isFiltering = true;
    this.page = 0;
    this.searchCategories();
  }

  resetFilters(): void {
    this.currentFilter = CategoryFilter.empty();
    this.currentFilters = this.currentFilter.toForm();
    this.isFiltering = false;
    this.page = 0;
    this.error = '';
    this.loadCategories();
  }

  changePage(newPage: number): void {
    if (newPage < 0 || newPage >= this.totalPages || newPage === this.page) {
      return;
    }

    this.page = newPage;
    this.refresh();
  }

  changeSize(newSize: number): void {
    if (!Number.isFinite(newSize) || newSize <= 0 || newSize === this.size) {
      return;
    }

    this.size = newSize;
    this.page = 0;
    this.refresh();
  }

  toggleSelectAll(checked: boolean): void {
    this.selection = this.selection.togglePage(this.categories, checked);
    this.selectedIds = this.selection.toSet();
  }

  toggleSelectItem(item: Category, checked: boolean): void {
    this.selection = this.selection.toggle(item.id, checked);
    this.selectedIds = this.selection.toSet();
  }

  setSelectedFile(file: File | null): void {
    this.selectedFile = file;
  }

  clearSelectedFile(): void {
    this.selectedFile = null;
  }

  applyBatchResponse(response: CategoryBatchActionResponse): void {
    this.applyUpdatedStatusRows(response.updated ?? []);
  }

  refresh(): void {
    if (this.isFiltering) {
      this.searchCategories();
      return;
    }

    this.loadCategories();
  }

  private searchCategories(): void {
    this.loading = true;
    this.error = '';

    this.categoryService
      .search(this.currentFilter.toSearchRequest(), this.page, this.size)
      .subscribe({
        next: (response) => {
          this.applyPageResponse(response);
          this.loading = false;
        },
        error: (err) => {
          console.error('Lỗi khi tìm kiếm category:', err);
          this.error = 'Không tìm kiếm được dữ liệu';
          this.loading = false;
          this.ui.error('Tìm kiếm thất bại');
        },
      });
  }

  private clearSelection(): void {
    this.selection = this.selection.clear();
    this.selectedIds = this.selection.toSet();
  }

  private applyPageResponse(response: CategoryPageResponse): void {
    this.categories = response.content ?? [];
    this.totalElements = response.totalElements ?? 0;
    this.totalPages = response.totalPages ?? 0;
    this.page = response.page ?? 0;
    this.clearSelection();
  }

  private applyUpdatedStatusRows(updatedRows: { id: number; status: number }[]): void {
    const updatedMap = new Map<number, number>();

    updatedRows.forEach((row) => updatedMap.set(row.id, row.status));

    const touchedRows: Category[] = [];
    const untouchedRows: Category[] = [];
    let removedCount = 0;

    for (const item of this.categories) {
      const entity = CategoryEntity.fromModel(item);

      if (entity.id == null || !updatedMap.has(entity.id)) {
        untouchedRows.push(item);
        continue;
      }

      const merged = entity.withStatus(updatedMap.get(entity.id)!).raw;

      if (CategoryEntity.fromModel(merged).shouldRemainVisible(this.currentFilter)) {
        touchedRows.push(merged);
      } else {
        removedCount += 1;
      }
    }

    this.categories = [...touchedRows, ...untouchedRows];

    if (removedCount > 0) {
      this.totalElements = Math.max(this.totalElements - removedCount, 0);
      this.totalPages = this.totalElements > 0 ? Math.ceil(this.totalElements / this.size) : 0;
    }

    this.clearSelection();
  }
}
