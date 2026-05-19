import { Category } from '../../models/category.models';

export class CategorySelection {
  constructor(private readonly ids: ReadonlySet<number> = new Set<number>()) {}

  static empty(): CategorySelection {
    return new CategorySelection();
  }

  toSet(): Set<number> {
    return new Set(this.ids);
  }

  clear(): CategorySelection {
    return CategorySelection.empty();
  }

  toggle(id: number | null | undefined, checked: boolean): CategorySelection {
    if (id == null) {
      return this;
    }

    const next = this.toSet();

    if (checked) {
      next.add(id);
    } else {
      next.delete(id);
    }

    return new CategorySelection(next);
  }

  togglePage(items: Category[], checked: boolean): CategorySelection {
    const next = this.toSet();

    for (const item of items) {
      if (item.id == null) {
        continue;
      }

      if (checked) {
        next.add(item.id);
      } else {
        next.delete(item.id);
      }
    }

    return new CategorySelection(next);
  }

  selectedItems(items: Category[]): Category[] {
    return items.filter((item) => item.id != null && this.ids.has(item.id));
  }

  allSelectedOnPage(items: Category[]): boolean {
    const pageIds = items.map((item) => item.id).filter((id): id is number => id != null);
    return pageIds.length > 0 && pageIds.every((id) => this.ids.has(id));
  }

  someSelectedOnPage(items: Category[]): boolean {
    const pageIds = items.map((item) => item.id).filter((id): id is number => id != null);
    const selectedCount = pageIds.filter((id) => this.ids.has(id)).length;
    return selectedCount > 0 && selectedCount < pageIds.length;
  }
}
