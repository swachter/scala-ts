import { Component } from '@angular/core';
import { CounterClient } from 'scala-client';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'webapp';
  counterValue: number;

  constructor() {
    this.updateCurrentValue()
  }

  private updateCurrentValue(): void {
    CounterClient.currentValue().then(counter => this.counterValue = counter.value)
  }

  increment(): void {
    CounterClient.increment(1).then(_ => this.updateCurrentValue());
  }

}
