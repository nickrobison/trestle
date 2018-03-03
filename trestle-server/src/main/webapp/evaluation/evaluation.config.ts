import { COLOR_SCHEME, IColorServiceConfig } from "../workspace/SharedModule/color/color-service.config";
import { ICacheServiceConfig } from "../workspace/SharedModule/cache/cache.service.config";

export const COLOR_EVAL_CONFIG: IColorServiceConfig = {
    colorType: COLOR_SCHEME.CATEGORY_20C
};

export const CACHE_EVAL_CONFIG: ICacheServiceConfig = {
    maxAge: 30000,
    maxSize: 1000
};
