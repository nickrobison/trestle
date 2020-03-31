import {ICacheServiceConfig} from '../shared/cache/cache.service.config';
import {InjectionToken} from '@angular/core';

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

export const COLOR_DI_CONFIG: IColorServiceConfig = {
  colorType: COLOR_SCHEME.CATEGORY_20B
};

export const INDIVIDUAL_CACHE_DI_CONFIG: ICacheServiceConfig = {
  maxAge: 30000,
  maxSize: 1000
};
