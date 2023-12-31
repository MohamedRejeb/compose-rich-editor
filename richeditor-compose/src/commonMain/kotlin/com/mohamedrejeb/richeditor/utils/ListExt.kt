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
 * Returns `true` if at least one element matches the given [predicate].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastAny(predicate: (T) -> Boolean): Boolean {
  contract { callsInPlace(predicate) }
  fastForEach { if (predicate(it)) return true }
  return false
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