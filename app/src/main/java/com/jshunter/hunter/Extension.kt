package com.jshunter.hunter

fun retry(times: Int, action: (index: Int) -> Boolean) {
    var result: Boolean
    var index = 0
    do {
        result = action(index)
        index++
    } while (!result && index < times)
}

fun repeatUntilFailure(
    maxAttemptTimes: Int,
    action: () -> Boolean,
    onInterval: (attemptTimes: Int) -> Unit
) {
    var attempt = 0
    while (true) {
        if (action()) attempt = 0 else attempt++
        if (attempt >= maxAttemptTimes) break
        onInterval(attempt)
    }
}