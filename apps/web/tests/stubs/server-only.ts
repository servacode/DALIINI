// `server-only` throws on import outside a Next server bundle. Vitest runs these modules
// directly in Node, where that guard has nothing to protect, so it is aliased to nothing.
export {};
