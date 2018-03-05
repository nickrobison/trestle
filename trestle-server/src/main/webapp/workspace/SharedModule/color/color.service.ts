import { Inject, Injectable } from "@angular/core";
import { COLOR_SCHEME, COLOR_SERVICE_CONFIG, IColorServiceConfig } from "./color-service.config";
import { schemeCategory10, schemeCategory20, schemeCategory20b, schemeCategory20c } from "d3-scale";


@Injectable()
export class ColorService {

    private availableColors: string[];
    private colorScale: string[];

    public constructor(@Inject(COLOR_SERVICE_CONFIG) private config: IColorServiceConfig) {
        this.availableColors = [];
        this.colorScale = this.initializeColorScale();
    }

    /**
     * Get a color from the color scale, if one is available
     * If not, return white
     * @param {number} layer number to use when getting color
     * @param {string} fallbackColor to use if none are avilable
     * @returns {string}
     */
    public getColor(layer: number, fallbackColor = "white"): string {
        // See if we have a color available
        const aColor = this.availableColors.pop();
        if (aColor === undefined) {
            const color = this.colorScale[layer];
            if (color === null) {
                return fallbackColor;
            }
            return color;
        }
        return aColor;
    }

    /**
     * Return a color so that it's available for the next layer
     * @param {string} color
     */
    public returnColor(color: string): void {
        this.availableColors.push(color);
    }

    /**
     * Reset everything
     */
    public reset(): void {
        this.availableColors = [];
        this.colorScale = this.initializeColorScale();
    }

    private initializeColorScale(): string[] {
        switch (this.config.colorType) {
            case COLOR_SCHEME.CATEGORY_20B: {
                return schemeCategory20b;
            }
            case COLOR_SCHEME.CATEGORY_20C: {
                return schemeCategory20c;
            }
            case COLOR_SCHEME.CATEGORY_20: {
                return schemeCategory20;
            }
            case COLOR_SCHEME.CATEGORY_10: {
                return schemeCategory10;
            }
        }
    }
}
