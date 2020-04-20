/**
 * Created by nrobison on 1/19/17.
 */
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {AuthService} from '../../user/authentication.service';
import {State} from '../../reducers';
import {select, Store} from '@ngrx/store';
import {Subscription} from 'rxjs';
import {login} from '../../actions/auth.actions';

interface IUserLogin {
  username: string;
  password: string;
}

@Component({
  selector: 'login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  animations: [
    trigger('errorMessage', [
      state('inactive', style({
        backgroundColor: 'white'
      })),
      state('active', style({
        backgroundColor: 'red'
      })),
      transition('inactive => active', animate('100ms ease-in')),
      transition('active => inactive', animate('100ms ease-out'))
    ])
  ]
})

export class LoginComponent implements OnInit {
  public loginForm: FormGroup;
  public errorMessage: string;
  public errorState: string;
  private returnUrl: string;
  private errorSubscription: Subscription;

  constructor(private fb: FormBuilder, private authService: AuthService, private route: ActivatedRoute, private router: Router, private store: Store<State>) {
    // Not used
  }

  public ngOnInit(): void {

    this.errorSubscription = this.store
      .pipe(select('user'), select('userError'))
      .subscribe((result) => {
        if (result) {
          this.errorState = 'active';
          this.errorMessage = result.message;
        } else {
          this.errorState = 'inactive';
          this.errorMessage = '';
        }
      });

    this.authService.logout();
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
    this.loginForm = this.fb.group({
      username: [null, Validators.required],
      password: [null, Validators.required]
    });
    this.errorMessage = '';
    this.errorState = 'inactive';
  }

  /**
   * Attempt to login the given user
   * @param {IUserLogin} user
   */
  public login(user: IUserLogin) {
    this.store.dispatch(login({username: user.username, password: user.password}));
    this.authService.login(user.username, user.password).subscribe(() => {
      // this.eventBus.publish(new UserLoginEvent(true));
      this.router.navigate([this.returnUrl]);
    }, (error: Response) => {
      console.debug('Logged?', error);
      console.error('Error logging in: ', error.status);
      if (error.status == 401) {
        this.errorState = 'active';
        this.errorMessage = 'Incorrect Username or Password';
      }
    });
  }
}
