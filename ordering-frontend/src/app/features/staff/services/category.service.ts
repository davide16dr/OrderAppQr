import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface TenantCategory {
  id: number;
  name: string;
  description: string;
  displayOrder: number;
  status: 'ACTIVE' | 'DISABLED';
}

export interface CreateCategoryRequest {
  name: string;
  description?: string;
  displayOrder?: number;
}

export interface UpdateCategoryRequest {
  name?: string;
  description?: string;
  displayOrder?: number;
  status?: 'ACTIVE' | 'DISABLED';
}

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/dashboard/categories`;

  getCategories(): Observable<TenantCategory[]> {
    return this.http.get<TenantCategory[]>(this.baseUrl);
  }

  getCategoryById(categoryId: number): Observable<TenantCategory> {
    return this.http.get<TenantCategory>(`${this.baseUrl}/${categoryId}`);
  }

  createCategory(request: CreateCategoryRequest): Observable<TenantCategory> {
    return this.http.post<TenantCategory>(this.baseUrl, request);
  }

  updateCategory(categoryId: number, request: UpdateCategoryRequest): Observable<TenantCategory> {
    return this.http.patch<TenantCategory>(`${this.baseUrl}/${categoryId}`, request);
  }

  deleteCategory(categoryId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${categoryId}`);
  }
}
