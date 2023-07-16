package com.mohamedrejeb.richeditor.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Copied from [androidx](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:text/text/src/main/java/androidx/compose/ui/text/android/TempListUtils.kt;l=33;drc=b2e3d878411b7fb1147455b1a204cddb7bee1a1b).
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
  contract { callsInPlace(action) }
  for (index in indices) {
    val item = get(index)
    action(item)
  }
}

@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastForEachIndexed(action: (index: Int, T) -> Unit) {
  contract { callsInPlace(action) }
  for (index in indices) {
    val item = get(index)
    action(index, item)
  }
}

@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastFirstOrNull(predicate: (T) -> Boolean): T? {
  contract { callsInPlace(predicate) }
  for (index in indices) {
    val item = get(index)
    if (predicate(item)) {
      return item
    }
  }
  return null
}

/**
 * Copied from [androidx](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:text/text/src/main/java/androidx/compose/ui/text/android/TempListUtils.kt;l=50;drc=b2e3d878411b7fb1147455b1a204cddb7bee1a1b).
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastMap(
  transform: (T) -> R
): List<R> {
  contract { callsInPlace(transform) }
  val destination = ArrayList<R>(/* initialCapacity = */ size)
  fastForEach { item ->
    destination.add(transform(item))
  }
  return destination
}

/**
 * Copied from [androidx](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-text/src/commonMain/kotlin/androidx/compose/ui/text/TempListUtils.kt;l=107;drc=ceaa7640c065146360515e598a3d09f6f66553dd).
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastFold(initial: R, operation: (acc: R, T) -> R): R {
  contract { callsInPlace(operation) }
  var accumulator = initial
  fastForEach { e ->
    accumulator = operation(accumulator, e)
  }
  return accumulator
}

@OptIn(ExperimentalContracts::class)
internal inline fun <R> fastMapRange(
  start: Int,
  end: Int,
  transform: (Int) -> R
): List<R> {
  contract { callsInPlace(transform) }
  val destination = ArrayList<R>(/* initialCapacity = */ end - start + 1)
  for (i in start..end) {
    destination.add(transform(i))
  }
  return destination
}