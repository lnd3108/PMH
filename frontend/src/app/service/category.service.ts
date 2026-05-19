import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { CategorySearchRequest } from '../domain/category/category-filter';
import { Category } from '../models/category.models';

export interface ApiResponse<T> {
  message?: string;
  data: T;
}

export interface CategoryPageResponse {
  content: Category[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  sortBy?: string | null;
  sortDir?: string | null;
}

export interface CategoryStatusOnlyResponse {
  id: number;
  status: number;
}

export interface CategoryBatchError {
  id: number | null;
  code: string;
  message: string;
}

export interface CategoryBatchActionResponse {
  totalRequested: number;
  successCount: number;
  failedCount: number;
  updated: CategoryStatusOnlyResponse[];
  failed: CategoryBatchError[];
}

export interface CategoryExportRequest extends CategorySearchRequest {
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface CategoryImportExcelError {
  rowNumber: number;
  message: string;
}

export interface CategoryImportExcelResponse {
  totalRows: number;
  successRows: number;
  failedRows: number;
  errorRows?: number;
  errors: CategoryImportExcelError[];
}

type ComponentCodeResponseItem =
  | string
  | { code?: unknown; name?: unknown; componentCode?: unknown };

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8094/api/jpa/categories';

  getAll(page = 0, size = 20): Observable<CategoryPageResponse> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .set('sortBy', 'id')
      .set('sortDir', 'desc');

    return this.http
      .get<ApiResponse<CategoryPageResponse>>(this.apiUrl, { params })
      .pipe(map((res) => res.data));
  }

  getById(id: number): Observable<Category> {
    return this.http
      .get<ApiResponse<Category>>(`${this.apiUrl}/${id}`)
      .pipe(map((res) => res.data));
  }

  getComponentCodes(): Observable<string[]> {
    const params = new HttpParams().set('isActive', '1');

    return this.http
      .get<
        ApiResponse<ComponentCodeResponseItem[]> | ComponentCodeResponseItem[]
      >(`${this.apiUrl}/component-codes`, { params })
      .pipe(map((res) => this.normalizeComponentCodes('data' in res ? res.data : res)));
  }

  create(data: Category): Observable<Category> {
    return this.http.post<ApiResponse<Category>>(this.apiUrl, data).pipe(map((res) => res.data));
  }

  update(id: number, data: Category): Observable<Category> {
    return this.http
      .put<ApiResponse<Category>>(`${this.apiUrl}/${id}`, data)
      .pipe(map((res) => res.data));
  }

  delete(id: number): Observable<string> {
    return this.http
      .delete<ApiResponse<string>>(`${this.apiUrl}/${id}`)
      .pipe(map((res) => res.data));
  }

  deleteBatch(ids: number[]): Observable<CategoryBatchActionResponse> {
    return this.http
      .delete<ApiResponse<CategoryBatchActionResponse>>(`${this.apiUrl}/batch`, {
        body: { ids },
      })
      .pipe(map((res) => res.data));
  }

  search(data: CategorySearchRequest, page = 0, size = 20): Observable<CategoryPageResponse> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .set('sortBy', 'id')
      .set('sortDir', 'desc');

    return this.http
      .post<ApiResponse<CategoryPageResponse>>(`${this.apiUrl}/search`, data, { params })
      .pipe(map((res) => res.data));
  }

  submit(id: number): Observable<Category> {
    return this.http
      .post<ApiResponse<Category>>(`${this.apiUrl}/${id}/submit`, {})
      .pipe(map((res) => res.data));
  }

  createAndSubmit(data: Category): Observable<Category> {
    return this.http
      .post<ApiResponse<Category>>(`${this.apiUrl}/submit-create`, data)
      .pipe(map((res) => res.data));
  }

  approve(id: number): Observable<Category> {
    return this.http
      .post<ApiResponse<Category>>(`${this.apiUrl}/${id}/approve`, {})
      .pipe(map((res) => res.data));
  }

  reject(id: number, data: { reason: string }): Observable<Category> {
    return this.http
      .post<ApiResponse<Category>>(`${this.apiUrl}/${id}/reject`, data)
      .pipe(map((res) => res.data));
  }

  cancelApprove(id: number): Observable<Category> {
    return this.http
      .post<ApiResponse<Category>>(`${this.apiUrl}/${id}/cancel-approve`, {})
      .pipe(map((res) => res.data));
  }

  submitBatch(ids: number[]): Observable<CategoryBatchActionResponse> {
    return this.http
      .post<ApiResponse<CategoryBatchActionResponse>>(`${this.apiUrl}/submit-batch`, { ids })
      .pipe(map((res) => res.data));
  }

  approveBatch(ids: number[]): Observable<CategoryBatchActionResponse> {
    return this.http
      .post<ApiResponse<CategoryBatchActionResponse>>(`${this.apiUrl}/approve-batch`, { ids })
      .pipe(map((res) => res.data));
  }

  cancelApproveBatch(ids: number[]): Observable<CategoryBatchActionResponse> {
    return this.http
      .post<
        ApiResponse<CategoryBatchActionResponse>
      >(`${this.apiUrl}/batch-cancel-approve`, { ids })
      .pipe(map((res) => res.data));
  }

  exportExcel(filters?: CategoryExportRequest): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/export`, filters || {}, {
      responseType: 'blob',
    });
  }

  downloadTemplateCategory(): Observable<HttpResponse<Blob>> {
    console.debug(
      '[CategoryService] downloadTemplateCategory URL',
      `${this.apiUrl}/download-template`,
    );

    return this.http.get(`${this.apiUrl}/download-template`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  importExcel(file: File): Observable<CategoryImportExcelResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('submitAfterImport', 'false');

    return this.http
      .post<ApiResponse<CategoryImportExcelResponse>>(`${this.apiUrl}/import`, formData)
      .pipe(map((res) => res.data));
  }

  private normalizeComponentCodes(value: ComponentCodeResponseItem[] | null | undefined): string[] {
    if (!Array.isArray(value)) {
      return [];
    }

    const codes = value
      .map((item) => {
        if (typeof item === 'string') {
          return item.trim();
        }

        const code = item.code ?? item.componentCode ?? item.name;

        return typeof code === 'string' ? code.trim() : '';
      })
      .filter((code): code is string => !!code);

    return Array.from(new Set(codes));
  }
}
