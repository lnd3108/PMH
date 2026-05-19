import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
  isDevMode,
} from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import {
  ColDef,
  GetRowIdParams,
  GridApi,
  GridReadyEvent,
  ICellRendererParams,
  RowClickedEvent,
  RowSelectionOptions,
  SelectionColumnDef,
  SelectionChangedEvent,
  themeQuartz,
} from 'ag-grid-community';
import { TuiPagination } from '@taiga-ui/kit/components/pagination';

import { CategoryEntity } from '../../../../domain/category/category.entity';
import { CategoryStatus, CategoryStatusPolicy } from '../../../../domain/category/category-status';
import { Category } from '../../../../models/category.models';
import { CategoryPermissionService } from '../../../../service/category-permission.service';

type RowAction = {
  key: 'edit' | 'copy' | 'submit' | 'approve' | 'cancelApprove' | 'delete';
  label: string;
  tone: 'neutral' | 'primary' | 'success' | 'secondary' | 'danger';
};

type ActionMenuState = {
  item: Category;
  top: number;
  left: number;
};

@Component({
  selector: 'app-category-table',
  standalone: true,
  imports: [CommonModule, AgGridAngular, TuiPagination],
  templateUrl: './category-table.html',
  styleUrl: './category-table.css',
})
export class CategoryTableComponent implements OnChanges {
  private readonly permissionService = inject(CategoryPermissionService);

  @Input() categories: Category[] = [];
  @Input() loading = false;
  @Input() error = '';
  @Input() page = 0;
  @Input() size = 20;
  @Input() totalElements = 0;
  @Input() totalPages = 0;
  @Input() selectedIds = new Set<number>();
  @Input() allSelected = false;
  @Input() someSelected = false;

  @Output() viewDetail = new EventEmitter<Category>();
  @Output() edit = new EventEmitter<Category>();
  @Output() delete = new EventEmitter<Category>();
  @Output() copy = new EventEmitter<Category>();
  @Output() viewSubmit = new EventEmitter<Category>();
  @Output() viewApprove = new EventEmitter<Category>();
  @Output() viewCancelApprove = new EventEmitter<Category>();
  @Output() toggleAll = new EventEmitter<boolean>();
  @Output() toggleItem = new EventEmitter<{ item: Category; checked: boolean }>();
  @Output() pageChange = new EventEmitter<number>();
  @Output() sizeChange = new EventEmitter<number>();

  private gridApi?: GridApi<Category>;
  private syncingSelection = false;
  private readonly fallbackRowIds = new WeakMap<Category, string>();
  private fallbackRowIdSequence = 0;
  activeActionMenu: ActionMenuState | null = null;

  readonly pageSizeOptions = [20, 50, 100];

  readonly agTheme = themeQuartz.withParams({
    accentColor: '#1f6f6d',
    fontFamily:
      "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    fontSize: 14,
    backgroundColor: '#ffffff',
    foregroundColor: '#0f2747',
    headerBackgroundColor: '#f8fafc',
    headerTextColor: '#587091',
    borderColor: '#dde5ef',
    rowHoverColor: '#f8fafc',
    selectedRowBackgroundColor: '#eefafa',
    wrapperBorderRadius: 22,
  });

  readonly getRowId = (params: GetRowIdParams<Category>): string => {
    const id = params.data.id;

    if (id != null) {
      return String(id);
    }

    const existingFallbackId = this.fallbackRowIds.get(params.data);

    if (existingFallbackId) {
      return existingFallbackId;
    }

    const fallbackId = `category-row-${++this.fallbackRowIdSequence}`;
    this.fallbackRowIds.set(params.data, fallbackId);

    return fallbackId;
  };

  readonly rowSelection: RowSelectionOptions = {
    mode: 'multiRow',
    checkboxes: true,
    headerCheckbox: true,
    enableClickSelection: false,
  };

  readonly selectionColumnDef: SelectionColumnDef = {
    pinned: 'left',
    lockPinned: true,
    lockPosition: 'left',
    width: 44,
    minWidth: 44,
    maxWidth: 44,
    resizable: false,
    sortable: false,
    suppressMovable: true,
    cellClass: 'ag-pinned-left-cell',
    headerClass: 'ag-pinned-left-cell',
  };

  readonly defaultColDef: ColDef<Category> = {
    sortable: true,
    resizable: true,
    filter: false,
    minWidth: 120,
  };

  readonly columnDefs: ColDef<Category>[] = [
    {
      headerName: 'STT',
      valueGetter: (params) => this.getRowNumber(params.node?.rowIndex ?? 0),
      width: 56,
      minWidth: 56,
      maxWidth: 56,
      pinned: 'left',
      lockPinned: true,
      lockPosition: 'left',
      sortable: false,
      filter: false,
      resizable: false,
      suppressMovable: true,
      cellClass: 'ag-pinned-left-cell',
      headerClass: 'ag-pinned-left-cell',
    },
    {
      headerName: 'Danh mục theo nhóm',
      field: 'paramType',
      width: 180,
      minWidth: 180,
      pinned: 'left',
      lockPinned: true,
      lockPosition: 'left',
      suppressMovable: true,
      cellClass: 'ag-pinned-left-cell',
      headerClass: 'ag-pinned-left-cell',
      valueFormatter: (params) => params.value || '-',
    },
    {
      headerName: 'Giá trị thành phần',
      field: 'paramValue',
      width: 170,
      minWidth: 170,
      pinned: 'left',
      lockPinned: true,
      lockPosition: 'left',
      suppressMovable: true,
      cellClass: 'ag-pinned-left-cell ag-pinned-left-boundary',
      headerClass: 'ag-pinned-left-cell ag-pinned-left-boundary',
      valueFormatter: (params) => params.value || '-',
    },
    {
      headerName: 'Tên thành phần',
      field: 'paramName',
      minWidth: 180,
      valueFormatter: (params) => params.value || '-',
    },
    {
      headerName: 'Mô tả',
      field: 'description',
      minWidth: 220,
      flex: 1,
      valueFormatter: (params) => params.value || '-',
    },
    {
      headerName: 'Cấu phần xử lý',
      field: 'componentCode',
      minWidth: 150,
      valueFormatter: (params) => params.value || '-',
    },
    {
      headerName: 'Ngày hiệu lực',
      field: 'effectiveDate',
      minWidth: 150,
      valueFormatter: (params) => params.value || '-',
    },
    {
      headerName: 'Ngày hết hiệu lực',
      field: 'endEffectiveDate',
      minWidth: 170,
      valueFormatter: (params) => params.value || '-',
    },
    {
      headerName: 'Trạng thái tham số',
      field: 'status',
      minWidth: 170,
      cellRenderer: (params: ICellRendererParams<Category, number>) => {
        const status = params.value ?? 0;
        const label = this.getStatusLabel(status);
        const className = this.getStatusClass(status);

        return `<span class="status-pill ${className}">${label}</span>`;
      },
    },
    {
      headerName: 'Trạng thái hoạt động',
      field: 'isActive',
      minWidth: 170,
      cellRenderer: (params: ICellRendererParams<Category, number>) => {
        const active = params.value === 1;
        const label = active ? 'Hoạt động' : 'Không hoạt động';
        const className = active ? 'status-pill--active' : 'status-pill--inactive';

        return `<span class="status-pill ${className}">${label}</span>`;
      },
    },
    {
      headerName: 'Hành động',
      colId: 'actions',
      width: 112,
      minWidth: 112,
      maxWidth: 124,
      pinned: 'right',
      sortable: false,
      filter: false,
      resizable: false,
      suppressMovable: true,
      cellClass: 'ag-actions-cell',
      cellRenderer: (params: ICellRendererParams<Category>) => {
        const item = params.data;

        if (!item) {
          return '';
        }

        return `
          <div class="ag-row-actions">
            <button
              type="button"
              class="ag-action-trigger"
              data-action-menu="true"
              aria-haspopup="menu"
              aria-label="Mở menu hành động"
            >
              <span>Thao tác</span>
              <span class="ag-action-trigger__icon" aria-hidden="true">▾</span>
            </button>
          </div>
        `;
      },
      onCellClicked: (params) => {
        const event = params.event as MouseEvent;
        const target = event.target as HTMLElement;
        const trigger = target.closest<HTMLButtonElement>('[data-action-menu]');

        if (trigger && params.data) {
          event.preventDefault();
          event.stopPropagation();
          this.toggleActionMenu(params.data, trigger);
          return;
        }

        const button = target.closest<HTMLButtonElement>('[data-action]');

        if (!button || !params.data) {
          return;
        }

        event.preventDefault();
        event.stopPropagation();

        switch (button.dataset['action']) {
          case 'edit':
            this.edit.emit(params.data);
            break;

          case 'copy':
            this.copy.emit(params.data);
            break;

          case 'submit':
            this.viewSubmit.emit(params.data);
            break;

          case 'approve':
            this.viewApprove.emit(params.data);
            break;

          case 'cancelApprove':
            this.viewCancelApprove.emit(params.data);
            break;

          case 'delete':
            this.delete.emit(params.data);
            break;
        }
      },
    },
  ];

  get startItem(): number {
    if (this.totalElements === 0) {
      return 0;
    }

    return this.page * this.size + 1;
  }

  get endItem(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['loading'] || changes['categories']) {
      this.closeActionMenu();
      this.scheduleGridRefresh();
    }

    if (changes['categories'] || changes['selectedIds']) {
      queueMicrotask(() => this.syncGridSelection());
    }
  }

  onGridReady(event: GridReadyEvent<Category>): void {
    this.gridApi = event.api;
    this.scheduleGridRefresh();
    this.syncGridSelection();
  }

  onRowClicked(event: RowClickedEvent<Category>): void {
    const target = event.event?.target as HTMLElement | null;

    if (!event.data || this.isInteractiveGridTarget(target)) {
      return;
    }

    this.closeActionMenu();
    this.viewDetail.emit(event.data);
  }

  onSelectionChanged(event: SelectionChangedEvent<Category>): void {
    if (this.syncingSelection) {
      return;
    }

    const selectedIdsInGrid = new Set(
      event.api
        .getSelectedRows()
        .map((item) => item.id)
        .filter((id): id is number => id != null),
    );

    for (const item of this.categories) {
      if (item.id == null) {
        continue;
      }

      const shouldBeSelected = selectedIdsInGrid.has(item.id);
      const wasSelected = this.selectedIds.has(item.id);

      if (shouldBeSelected !== wasSelected) {
        this.toggleItem.emit({
          item,
          checked: shouldBeSelected,
        });
      }
    }
  }

  onSelectPage(page: number): void {
    if (page !== this.page) {
      this.closeActionMenu();
      this.pageChange.emit(page);
    }
  }

  onSizeSelect(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);

    if (Number.isFinite(value) && value > 0) {
      this.closeActionMenu();
      this.sizeChange.emit(value);
    }
  }

  getRowNumber(index: number): number {
    return this.page * this.size + index + 1;
  }

  getStatusLabel(status: number): string {
    return CategoryStatusPolicy.label(status);
  }

  getStatusClass(status: number): string {
    switch (status) {
      case CategoryStatus.New:
        return 'status-pill--draft';

      case CategoryStatus.PendingApproval:
        return 'status-pill--pending';

      case CategoryStatus.Approved:
        return 'status-pill--approved';

      case CategoryStatus.Rejected:
        return 'status-pill--rejected';

      case CategoryStatus.CancelApproved:
        return 'status-pill--cancel-approved';

      default:
        return 'status-pill--inactive';
    }
  }

  getActiveClass(isActive?: number | null): string {
    return isActive === 1 ? 'status-pill--active' : 'status-pill--inactive';
  }

  canSubmit(item: Category): boolean {
    return CategoryEntity.fromModel(item).canSubmit();
  }

  canEdit(item: Category): boolean {
    return CategoryEntity.fromModel(item).canEdit();
  }

  canApprove(item: Category): boolean {
    return CategoryEntity.fromModel(item).canApprove();
  }

  canCancelApprove(item: Category): boolean {
    return CategoryEntity.fromModel(item).canCancelApprove();
  }

  canDelete(item: Category): boolean {
    const entity = CategoryEntity.fromModel(item);
    const canDeletePermission = this.permissionService.can('delete');
    const canDelete = canDeletePermission && entity.canDelete();

    if (isDevMode() && Number(item.status) === CategoryStatus.New) {
      console.debug('[CategoryTable] canDelete row action', {
        id: item.id,
        status: item.status,
        isDisplay: item.isDisplay,
        newData: item.newData,
        parsedNewData: entity.parseNewData(),
        hasMeaningfulNewData: entity.hasNewData(),
        canDeletePermission,
        canDelete,
      });
    }

    return canDelete;
  }

  getRowActions(item: Category): RowAction[] {
    const actions: RowAction[] = [{ key: 'copy', label: 'Sao chép', tone: 'neutral' }];

    if (this.canEdit(item)) {
      actions.unshift({ key: 'edit', label: 'Sửa', tone: 'neutral' });
    }

    if (this.canSubmit(item)) {
      actions.push({ key: 'submit', label: 'Gửi duyệt', tone: 'primary' });
    }

    if (this.canApprove(item)) {
      actions.push({ key: 'approve', label: 'Phê duyệt', tone: 'success' });
    }

    if (this.canCancelApprove(item)) {
      actions.push({ key: 'cancelApprove', label: 'Hủy duyệt', tone: 'secondary' });
    }

    if (this.canDelete(item)) {
      actions.push({ key: 'delete', label: 'Xóa', tone: 'danger' });
    }

    return actions;
  }

  closeActionMenu(): void {
    this.activeActionMenu = null;
  }

  runRowAction(action: RowAction['key'], item: Category): void {
    this.closeActionMenu();

    switch (action) {
      case 'edit':
        this.edit.emit(item);
        break;

      case 'copy':
        this.copy.emit(item);
        break;

      case 'submit':
        this.viewSubmit.emit(item);
        break;

      case 'approve':
        this.viewApprove.emit(item);
        break;

      case 'cancelApprove':
        this.viewCancelApprove.emit(item);
        break;

      case 'delete':
        this.delete.emit(item);
        break;
    }
  }

  private toggleActionMenu(item: Category, trigger: HTMLElement): void {
    if (this.activeActionMenu?.item === item) {
      this.closeActionMenu();
      return;
    }

    const rect = trigger.getBoundingClientRect();
    const menuWidth = 168;
    const menuHeight = Math.min(280, this.getRowActions(item).length * 40 + 16);
    const gap = 6;
    const left = Math.max(8, Math.min(rect.right - menuWidth, window.innerWidth - menuWidth - 8));
    const bottomTop = rect.bottom + gap;
    const top =
      bottomTop + menuHeight <= window.innerHeight - 8
        ? bottomTop
        : Math.max(8, rect.top - menuHeight - gap);

    this.activeActionMenu = { item, top, left };
  }

  private scheduleGridRefresh(): void {
    queueMicrotask(() => {
      if (!this.gridApi || this.gridApi.isDestroyed()) {
        return;
      }

      this.gridApi.refreshHeader();
      this.gridApi.refreshCells({ force: true });
    });
  }

  private isInteractiveGridTarget(target: HTMLElement | null): boolean {
    return !!target?.closest(
      [
        '[data-action]',
        '[data-action-menu]',
        '.ag-row-actions',
        '.ag-action-trigger',
        '.ag-selection-checkbox',
        '.ag-checkbox',
        '.ag-checkbox-input-wrapper',
        'button',
        'input',
        'select',
        'textarea',
        'a',
      ].join(','),
    );
  }

  private syncGridSelection(): void {
    if (!this.gridApi || this.gridApi.isDestroyed()) {
      return;
    }

    this.syncingSelection = true;

    this.gridApi.forEachNode((node) => {
      const id = node.data?.id;
      node.setSelected(id != null && this.selectedIds.has(id));
    });

    this.syncingSelection = false;
  }
}
