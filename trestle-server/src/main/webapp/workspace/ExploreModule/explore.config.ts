import { COLOR_SCHEME, IColorServiceConfig } from "../SharedModule/color/color-service.config";
import { ICacheServiceConfig } from "../SharedModule/cache/cache.service.config";

export const COLOR_DI_CONFIG: IColorServiceConfig = {
    colorType: COLOR_SCHEME.CATEGORY_20B
};

export const CACHE_DI_CONFIG: ICacheServiceConfig = {
    maxAge: 30000,
    maxSize: 1000
};
