package no.uutilsynet.testlab2testing

/**
 * Konverterer ei liste av Result<E> til ei Result<List<E>>. Hvis alle elementa i lista er
 * Result.success, blir resultatet Result.success med ei liste av alle elementa. Hvis ein eller
 * fleire av elementa er Result.failure, blir resultatet Result.failure med den første feilen som
 * feilmelding.
 */
fun <E> List<Result<E>>.toSingleResult(): Result<List<E>> = runCatching {
  this.map { it.getOrThrow() }
}

/** Tilsvarende funksjon som List<Result<E>>.toSingleResult(), men for Map. */
fun <K, V> Map<K, Result<V>>.toSingleResult(): Result<Map<K, V>> = runCatching {
  this.mapValues { it.value.getOrThrow() }
}
