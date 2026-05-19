import { Category } from '../../models/category.models';
import { CategoryFilter } from './category-filter';
import { parseCategoryNewData } from './category-new-data';
import { CategoryStatusPolicy } from './category-status';

export class CategoryEntity {
  constructor(private readonly snapshot: Category) {}

  static fromModel(category: Category): CategoryEntity {
    return new CategoryEntity(category);
  }

  get id(): number | null {
    return this.snapshot.id ?? null;
  }

  get raw(): Category {
    return { ...this.snapshot };
  }

  canSubmit(): boolean {
    return CategoryStatusPolicy.canSubmit(this.snapshot.status);
  }

  canApprove(): boolean {
    return CategoryStatusPolicy.canApprove(this.snapshot.status);
  }

  canReject(): boolean {
    return CategoryStatusPolicy.canReject(this.snapshot.status);
  }

  canCancelApprove(): boolean {
    return CategoryStatusPolicy.canCancelApprove(this.snapshot.status);
  }

  canEdit(): boolean {
    return CategoryStatusPolicy.canEdit(this.snapshot.status);
  }

  canDelete(): boolean {
    return CategoryStatusPolicy.canDelete(
      this.snapshot.status,
      this.snapshot.isDisplay,
      this.hasNewData(),
    );
  }

  shouldRemainVisible(filter: CategoryFilter): boolean {
    return filter.matches(this.snapshot);
  }

  withStatus(status: number): CategoryEntity {
    return new CategoryEntity({
      ...this.snapshot,
      status,
    });
  }

  parseNewData(): Category | null {
    return parseCategoryNewData(this.snapshot.newData) as Category | null;
  }

  hasNewData(): boolean {
    return this.parseNewData() !== null;
  }
}
