import { InjectionToken } from "@angular/core";

export const COLOR_SERVICE_CONFIG = new InjectionToken<IColorServiceConfig>("color-service.config");

export enum COLOR_SCHEME {
    CATEGORY_20B,
    CATEGORY_20C,
    CATEGORY_20,
    CATEGORY_10
}

export interface IColorServiceConfig {
    colorType: COLOR_SCHEME;
}
