package no.uutilsynet.testlab2testing.loeysing

import java.net.URL

fun sameURL(aURL: URL, anotherURL: URL): Boolean =
    aURL.host == anotherURL.host && equalPaths(aURL, anotherURL) && equalQueries(aURL, anotherURL)

private fun equalPaths(enURL: URL, enAnnenURL: URL) =
    addSlashSuffix(enURL.path) == addSlashSuffix(enAnnenURL.path)

private fun equalQueries(enURL: URL, enAnnenURL: URL): Boolean {
  val a = enURL.query ?: ""
  val b = enAnnenURL.query ?: ""
  return a == b
}

private fun addSlashSuffix(path: String): String = if (path.endsWith("/")) path else "$path/"
