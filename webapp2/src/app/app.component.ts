/**
 * Created by nrobison on 1/19/17.
 */
import {Component, HostListener, ViewChild, ViewEncapsulation} from "@angular/core";
import {TrestleUser} from "./user/trestle-user";
import {Subscription} from "rxjs";
import {AuthService, Privileges} from "./user/authentication.service";
import {Router} from "@angular/router";
import {MatSidenav} from "@angular/material/sidenav";
import {MD5} from "crypto-js";

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.scss"],
  encapsulation: ViewEncapsulation.None
})
export class AppComponent {
  public gravatarURL: string;
  // We need this in order to access the Privileges enum from the template
  public Privileges = Privileges;
  public user: TrestleUser | null;

  @ViewChild("sidenav") public sideNav: MatSidenav;
  private loginSubscription: Subscription;

  constructor(private authService: AuthService,
              private router: Router
              // private eventBus: EventBus
  ) {
  }


  public ngOnInit(): void {
    // Get the current user, if it exists
    this.user = this.authService.getUser();
    this.checkMenu();
    // this.loginSubscription = this.eventBus.subscribe(UserLoginEvent).subscribe((event) => {
    //     console.debug("User event, is logged in?", event.isLoggedIn());
    //     if (event.isLoggedIn()) {
    //         this.user = this.authService.getUser();
    //     } else {
    //         this.user = null;
    //     }
    // });
  }

  public ngOnDestroy(): void {
    this.loginSubscription.unsubscribe();
  }

  /**
   * Attempt to login the user
   */
  public login(): void {
    if (this.user == null) {
      this.router.navigate(["/login"]);
    }
  }

  /**
   * Does the user have the required permissions?
   * @param {Privileges[]} requiredPrivs
   * @returns {boolean}
   */
  public userHasRequiredPermissions(requiredPrivs: Privileges[]): boolean {
    if (this.user == null) {
      return false;
    }
    return this.user.hasRequiredPrivileges(requiredPrivs);
  }

  /**
   * Get the Gravitar URL of the user
   * @returns {string}
   */
  public getGravatarURL(): string {
    if (this.gravatarURL == null) {
      const user = this.authService.getUser();
      if (user !== null) {
        const hash = MD5(user.email.trim().toLowerCase()).toString();
        this.gravatarURL = "https://www.gravatar.com/avatar/" + hash + "?d=identicon" + "&s=36";
        return this.gravatarURL;
      }
    }
    return this.gravatarURL;
  }

  /**
   * Logout the currently logged in user
   */
  public logout(): void {
    this.authService.logout();
    this.user = null;
  }

//    Resize function for sidenav
  @HostListener("window:resize", ["$event"])
  public onResize(event: any): void {
    this.checkMenu();
  }

  private checkMenu() {
    if (window.innerWidth <= 800) {
      this.sideNav.close();
    } else {
      this.sideNav.open();
    }
  }
}
