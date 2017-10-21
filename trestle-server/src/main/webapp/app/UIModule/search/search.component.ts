import {Component, EventEmitter, OnInit, Output} from "@angular/core";
import {MatAutocompleteSelectedEvent} from "@angular/material/autocomplete";
import {IndividualService} from "../../SharedModule/individual/individual.service";
import {Observable} from "rxjs/Observable";
import {FormControl} from "@angular/forms";

@Component({
    selector: "search",
    templateUrl: "./search.component.html",
    styleUrls: ["./search.component.css"]
})
export class SearchComponent implements OnInit {

    public options: Observable<string[]>;
    public individualName = new FormControl();
    @Output() public selected = new EventEmitter<string>();

    public constructor(private is: IndividualService) {
    }

    public ngOnInit(): void {
        this.options = this.individualName
            .valueChanges
            .debounceTime(400)
            .distinctUntilChanged()
            .switchMap((name) => this.is.searchForIndividual(name));
    }

    public displayFn(individualName: string): string {
        const strings = individualName.split("#");
        return strings[1];
    }

    public selectHandler = (event: MatAutocompleteSelectedEvent): void => {
        this.selected.next(event.option.value);
    }
}
