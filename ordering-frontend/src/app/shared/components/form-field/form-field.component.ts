import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, AbstractControl } from '@angular/forms';

@Component({
  selector: 'app-form-field',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div [class.form-group]="!isFullWidth" [class.form-group-full]="isFullWidth">
      <label [for]="fieldId" class="form-label">
        <span>{{ label }}</span>
        @if (isRequired) {
          <span class="required">*</span>
        } @else {
          <span class="optional">(opzionale)</span>
        }
        @if (hint) {
          <span class="hint">{{ hint }}</span>
        }
      </label>

      @if (isSelect) {
        <div class="select-group">
          <span class="select-icon">{{ icon }}</span>
          <select
            [id]="fieldId"
            [value]="control.value || ''"
            (change)="onSelectionChange($event)"
            class="form-select"
            [class.is-valid]="isValid"
            [class.is-error]="isError"
          >
            <option value="" disabled selected>{{ placeholder }}</option>
            @for (option of options; track option.value) {
              <option [value]="option.value">{{ option.label }}</option>
            }
          </select>
          <span class="select-arrow">▼</span>
        </div>
      } @else {
        <div class="input-group">
          @if (icon) {
            <span class="input-icon">{{ icon }}</span>
          }
          @if (inputPrefix) {
            <span class="input-prefix">{{ inputPrefix }}</span>
          }
          <input
            [id]="fieldId"
            [type]="type"
            [value]="control.value || ''"
            (input)="onInputChange($event)"
            (blur)="control.markAsTouched()"
            [placeholder]="placeholder"
            class="form-input"
            [class.has-value]="hasValue"
            [class.is-valid]="isValid"
            [class.is-error]="isError"
            [attr.maxlength]="maxlength"
          />
          @if (isValid) {
            <div class="input-feedback">✓</div>
          }
        </div>
      }

      @if (errorMessage) {
        <div class="field-hint field-error">
          {{ errorMessage }}
        </div>
      } @else if (infoHint && !isError) {
        <div class="field-hint hint-info">
          {{ infoHint }}
        </div>
      }
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }
  `]
})
export class FormFieldComponent {
  @Input() fieldId!: string;
  @Input() label!: string;
  @Input() control!: AbstractControl;
  @Input() type: string = 'text';
  @Input() placeholder: string = '';
  @Input() icon: string = '';
  @Input() inputPrefix: string = '';
  @Input() isRequired: boolean = true;
  @Input() isFullWidth: boolean = false;
  @Input() isSelect: boolean = false;
  @Input() options: Array<{ value: string; label: string }> = [];
  @Input() maxlength: number | null = null;
  @Input() hint: string = '';
  @Input() infoHint: string = '';
  @Input() customError: string = '';

  onInputChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.control.setValue(target.value);
  }

  onSelectionChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.control.setValue(target.value);
  }

  get hasValue(): boolean {
    return !!this.control?.value;
  }

  get isValid(): boolean {
    return this.control?.valid && this.control?.touched && !this.customError;
  }

  get isError(): boolean {
    return (this.control?.invalid && this.control?.touched) || !!this.customError;
  }

  get errorMessage(): string {
    if (this.customError) return this.customError;
    
    const errors = this.control?.errors;
    if (!errors || !this.control?.touched) return '';

    if (errors['required']) return 'Campo obbligatorio';
    if (errors['minlength']) return `Minimo ${errors['minlength'].requiredLength} caratteri`;
    if (errors['maxlength']) return `Massimo ${errors['maxlength'].requiredLength} caratteri`;
    if (errors['email']) return 'Email non valida';
    if (errors['pattern']) {
      if (this.fieldId === 'postalCode') return 'CAP deve essere 5 cifre';
      if (this.fieldId === 'requestedSlug') return 'Solo lettere minuscole, numeri e trattini consentiti';
      return 'Formato non valido';
    }

    return '';
  }
}
