import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-signup-success',
  templateUrl: './signup-success.component.html',
  styleUrls: ['./signup-success.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class SignupSuccessComponent implements OnInit {
  countdownSeconds = 10;

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.startCountdown();
  }

  private startCountdown(): void {
    const interval = setInterval(() => {
      this.countdownSeconds--;
      if (this.countdownSeconds <= 0) {
        clearInterval(interval);
        this.navigateToLogin();
      }
    }, 1000);
  }

  navigateToLogin(): void {
    this.router.navigate(['/login']);
  }
}
