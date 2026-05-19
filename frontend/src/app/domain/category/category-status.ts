export const GROUP_CATEGORY_STATUS = {
  DRAFT: 1,
  PENDING: 3,
  APPROVED: 4,
  REJECTED: 5,
  CANCEL_APPROVED: 7,
} as const;

export enum CategoryStatus {
  New = 1,
  PendingApproval = 3,
  Approved = 4,
  Rejected = 5,
  CancelApproved = 7,
}

export class CategoryStatusPolicy {
  private static readonly submittableStatuses = new Set<number>([
    CategoryStatus.New,
    CategoryStatus.Rejected,
    CategoryStatus.CancelApproved,
  ]);

  private static readonly editableStatuses = new Set<number>([
    CategoryStatus.New,
    CategoryStatus.Rejected,
    CategoryStatus.CancelApproved,
  ]);

  static canSubmit(status: number | null | undefined): boolean {
    return status != null && this.submittableStatuses.has(status);
  }

  static canEdit(status: number | null | undefined): boolean {
    return status != null && this.editableStatuses.has(status);
  }

  static canApprove(status: number | null | undefined): boolean {
    return status === CategoryStatus.PendingApproval;
  }

  static canReject(status: number | null | undefined): boolean {
    return status === CategoryStatus.PendingApproval;
  }

  static canCancelApprove(status: number | null | undefined): boolean {
    return status === CategoryStatus.Approved;
  }

  static canDelete(
    status: number | null | undefined,
    isDisplay: number | null | undefined,
    hasNewData = false,
  ): boolean {
    if (status == null || hasNewData) {
      return false;
    }

    if (
      status === CategoryStatus.PendingApproval ||
      status === CategoryStatus.Approved ||
      status === CategoryStatus.CancelApproved
    ) {
      return false;
    }

    if (status === CategoryStatus.New) {
      return isDisplay == null || Number(isDisplay) !== 2;
    }

    if (status === CategoryStatus.Rejected) {
      return Number(isDisplay) === 1;
    }

    return false;
  }

  static isReadonly(status: number | null | undefined): boolean {
    return status === CategoryStatus.PendingApproval || status === CategoryStatus.Approved;
  }

  static label(status: number | null | undefined): string {
    switch (status) {
      case CategoryStatus.New:
        return 'Tạo mới';
      case CategoryStatus.PendingApproval:
        return 'Chờ phê duyệt';
      case CategoryStatus.Approved:
        return 'Đã phê duyệt';
      case CategoryStatus.Rejected:
        return 'Từ chối';
      case CategoryStatus.CancelApproved:
        return 'Hủy duyệt';
      default:
        return 'Không xác định';
    }
  }

  static badgeClass(status: number | null | undefined): string {
    switch (status) {
      case CategoryStatus.New:
        return 'bg-cyan-50 text-cyan-700';
      case CategoryStatus.PendingApproval:
        return 'bg-amber-50 text-amber-700';
      case CategoryStatus.Approved:
        return 'bg-green-50 text-green-700';
      case CategoryStatus.Rejected:
        return 'bg-red-50 text-red-700';
      case CategoryStatus.CancelApproved:
        return 'bg-slate-100 text-slate-700';
      default:
        return 'bg-slate-100 text-slate-700';
    }
  }
}
