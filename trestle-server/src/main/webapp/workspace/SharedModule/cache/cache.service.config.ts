import { InjectionToken } from "@angular/core";

export const CACHE_SERVICE_CONFIG = new InjectionToken<ICacheServiceConfig>("cache.service.config");
export interface ICacheServiceConfig {
    maxAge: number;
    maxSize: number;
}