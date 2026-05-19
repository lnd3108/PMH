import { Category } from '../../models/category.models';

export function parseCategoryNewData(value: unknown): Partial<Category> | null {
  if (value === null || value === undefined) {
    return null;
  }

  if (typeof value === 'string') {
    const trimmed = value.trim();

    if (!trimmed || trimmed === '{}' || trimmed.toLowerCase() === 'null') {
      return null;
    }

    try {
      const parsed = JSON.parse(trimmed) as unknown;

      return isMeaningfulObject(parsed) ? (parsed as Partial<Category>) : null;
    } catch {
      return { newData: trimmed };
    }
  }

  return isMeaningfulObject(value) ? (value as Partial<Category>) : null;
}

export function hasMeaningfulCategoryNewData(value: unknown): boolean {
  return parseCategoryNewData(value) !== null;
}

function isMeaningfulObject(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && Object.keys(value).length > 0;
}
