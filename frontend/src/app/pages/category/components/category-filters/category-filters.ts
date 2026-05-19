import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TuiButton } from '@taiga-ui/core/components/button';
import { TuiInput } from '@taiga-ui/core/components/input';
import { CategoryFilterForm } from '../../../../domain/category/category-filter';
import { TuiCheckbox, TuiDataList, TuiDropdown, TuiLabel } from '@taiga-ui/core';
import { TuiChevron } from '@taiga-ui/kit';

type FilterOption = { value: string | number; label: string };

@Component({
  selector: 'app-category-filters',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TuiInput,
    TuiButton,
    TuiLabel,
    TuiDropdown,
    TuiDataList,
    TuiChevron,
  ],
  templateUrl: './category-filters.html',
  styleUrl: './category-filters.css',
})
export class CategoryFiltersComponent implements OnChanges {
  @Input() statusOptions: FilterOption[] = [];
  @Input() activeOptions: FilterOption[] = [];

  @Input() currentFilters: CategoryFilterForm = {
    paramName: '',
    paramValue: '',
    paramType: '',
    status: [],
    isActive: [],
  };

  @Output() search = new EventEmitter<CategoryFilterForm>();
  @Output() reset = new EventEmitter<void>();

  filters: CategoryFilterForm = {
    paramName: '',
    paramValue: '',
    paramType: '',
    status: [],
    isActive: [],
  };

  statusDropdownOpen = false;
  activeDropdownOpen = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['currentFilters']) {
      this.filters = {
        paramName: this.currentFilters.paramName ?? '',
        paramValue: this.currentFilters.paramValue ?? '',
        paramType: this.currentFilters.paramType ?? '',
        status: (this.currentFilters.status ?? []).map(String),
        isActive: (this.currentFilters.isActive ?? []).map(String),
      };
    }
  }

  stopDropdownOptionClick(event: MouseEvent): void {
    event.stopPropagation();
  }

  closeDropdowns(): void {
    this.statusDropdownOpen = false;
    this.activeDropdownOpen = false;
  }

  onToggleMultiValue(field: 'status' | 'isActive', value: string | number, checked: boolean): void {
    const normalizedValue = String(value);
    const currentValues = this.filters[field].map(String);

    if (checked) {
      if (!currentValues.includes(normalizedValue)) {
        this.filters[field] = [...currentValues, normalizedValue];
      }

      return;
    }

    this.filters[field] = currentValues.filter((item) => item !== normalizedValue);
  }

  onStatusDropdownOpenChange(open: boolean): void {
    this.statusDropdownOpen = open;

    if (open) {
      this.activeDropdownOpen = false;
    }
  }

  onActiveDropdownOpenChange(open: boolean): void {
    this.activeDropdownOpen = open;

    if (open) {
      this.statusDropdownOpen = false;
    }
  }

  isChecked(field: 'status' | 'isActive', value: string | number): boolean {
    return this.filters[field].map(String).includes(String(value));
  }

  removeSelectedValue(field: 'status' | 'isActive', value: string | number): void {
    const normalizedValue = String(value);
    this.filters[field] = this.filters[field]
      .map(String)
      .filter((item) => item !== normalizedValue);
  }

  getSelectedOptions(field: 'status' | 'isActive', options: FilterOption[]): FilterOption[] {
    const values = this.filters[field].map(String);
    return options.filter(
      (option) => String(option.value) !== '' && values.includes(String(option.value)),
    );
  }

  hasSelectedValues(field: 'status' | 'isActive'): boolean {
    return this.filters[field].length > 0;
  }

  getSelectedLabels(field: 'status' | 'isActive', options: FilterOption[]): string {
    const values = this.filters[field].map(String);

    if (values.length === 0) {
      return '';
    }

    return options
      .filter((option) => String(option.value) !== '' && values.includes(String(option.value)))
      .map((option) => option.label)
      .join(', ');
  }

  onSearch(): void {
    this.search.emit({
      ...this.filters,
      status: this.filters.status.map(String),
      isActive: this.filters.isActive.map(String),
    });
    this.closeDropdowns();
  }

  onReset(): void {
    this.filters = {
      paramName: '',
      paramValue: '',
      paramType: '',
      status: [],
      isActive: [],
    };
    this.closeDropdowns();
    this.reset.emit();
  }
}
