import { NgModule } from "@angular/core";
import {
    MatAutocompleteModule, MatButtonModule, MatButtonToggleModule, MatCardModule, MatChipsModule,
    MatDialogModule,
    MatGridListModule, MatIconModule, MatInputModule, MatListModule,
    MatMenuModule, MatProgressBarModule, MatProgressSpinnerModule, MatSidenavModule,
    MatSliderModule, MatSlideToggleModule, MatTabsModule, MatToolbarModule
} from "@angular/material";

@NgModule({
    imports: [MatButtonModule,
        MatAutocompleteModule,
        MatDialogModule,
        MatSliderModule,
        MatSlideToggleModule,
        MatMenuModule,
        MatSidenavModule,
        MatCardModule,
        MatGridListModule,
        MatChipsModule,
        MatButtonToggleModule,
        MatToolbarModule,
        MatListModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatInputModule,
        MatTabsModule,
        MatProgressBarModule],
    exports: [MatButtonModule,
        MatAutocompleteModule,
        MatDialogModule,
        MatSliderModule,
        MatSlideToggleModule,
        MatMenuModule,
        MatSidenavModule,
        MatCardModule,
        MatGridListModule,
        MatChipsModule,
        MatButtonToggleModule,
        MatToolbarModule,
        MatListModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatInputModule,
        MatTabsModule,
        MatProgressBarModule]
})
export class MaterialModule {
}