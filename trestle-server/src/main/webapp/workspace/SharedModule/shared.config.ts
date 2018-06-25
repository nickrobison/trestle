import { ICacheServiceConfig } from "./cache/cache.service.config";

export const DATASET_CACHE_DI_CONFIG: ICacheServiceConfig = {
    maxAge: 600000,
    maxSize: 10
};