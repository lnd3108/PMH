import { Injectable, inject } from '@angular/core';
import { MeResponse } from '../models/auth.models';
import { AuthService } from './auth.service';

export type CategoryAction =
  | 'create'
  | 'edit'
  | 'copy'
  | 'submit'
  | 'approve'
  | 'cancelApprove'
  | 'delete'
  | 'export'
  | 'import';

@Injectable({
  providedIn: 'root',
})
export class CategoryPermissionService {
  private readonly authService = inject(AuthService);
  private readonly adminRoles = new Set(['ROLE_ADMIN', 'ADMIN', 'SUPER_ADMIN']);
  private readonly categoryScopeKeywords = [
    'CATEGORY',
    'CATEGORIES',
    'DANH_MUC',
    'THAM_SO',
    'PARAM',
  ];
  private readonly actionKeywords: Record<CategoryAction, string[]> = {
    create: ['CREATE', 'ADD'],
    edit: ['EDIT', 'UPDATE'],
    copy: ['COPY', 'CREATE', 'ADD'],
    submit: ['SUBMIT'],
    approve: ['APPROVE'],
    cancelApprove: ['CANCEL_APPROVE', 'CANCEL-APPROVE', 'CANCELAPPROVE'],
    delete: ['DELETE', 'REMOVE'],
    export: ['EXPORT'],
    import: ['IMPORT'],
  };

  can(action: CategoryAction): boolean {
    const user = this.authService.user();
    if (!user) {
      return false;
    }

    const normalizedRoles = (user.roles ?? []).map((role) => role.toUpperCase());
    if (normalizedRoles.some((role) => this.adminRoles.has(role))) {
      return true;
    }

    const authorities = this.getGrantedAuthorities(user);
    if (authorities.length === 0) {
      return true;
    }

    const actionTokens = this.actionKeywords[action];

    return authorities.some((authority) => {
      const inCategoryScope = this.categoryScopeKeywords.some((keyword) =>
        authority.includes(keyword),
      );
      const matchesAction = actionTokens.some((token) => authority.includes(token));
      return inCategoryScope && matchesAction;
    });
  }

  getActionLabel(action: CategoryAction): string {
    switch (action) {
      case 'create':
        return 'thêm mới';
      case 'edit':
        return 'chỉnh sửa';
      case 'copy':
        return 'sao chép';
      case 'submit':
        return 'gửi duyệt';
      case 'approve':
        return 'phê duyệt';
      case 'cancelApprove':
        return 'hủy duyệt';
      case 'delete':
        return 'xóa';
      case 'export':
        return 'xuất Excel';
      case 'import':
        return 'import Excel';
    }
  }

  getGrantedAuthorities(user: MeResponse | null = this.authService.user()): string[] {
    return [...(user?.roles ?? []), ...(user?.authorities ?? [])]
      .filter((value): value is string => !!value)
      .map((value) => value.toUpperCase());
  }
}
