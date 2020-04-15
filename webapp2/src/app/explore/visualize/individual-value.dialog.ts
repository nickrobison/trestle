/**
 * Created by nrobison on 3/16/17.
 */
import {Component} from '@angular/core';

@Component({
  selector: 'individual-value-dialog',
  template: '<h3 mat-dialog-title>{{name}}</h3>' +
    '<div mat-dialog-content>{{value}}</div> '
})
export class IndividualValueDialog {

  public name: string;
  public value: string;

  constructor() {
  }
}
