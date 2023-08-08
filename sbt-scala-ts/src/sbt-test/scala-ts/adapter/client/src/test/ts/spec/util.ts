  // combine delegate and adapter creation
  export function newAdapter<ARGS extends any[], DELEGATE, ADAPTER>(
    a: { newDelegate: (...args: ARGS) => DELEGATE, newAdapter: (d: DELEGATE) => ADAPTER },
    ...args: ARGS
  ): ADAPTER {
    return a.newAdapter(a.newDelegate(...args))
  }

