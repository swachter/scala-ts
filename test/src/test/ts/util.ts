export function assertNever(n: never): never {
    throw new Error('never case reached')
}
