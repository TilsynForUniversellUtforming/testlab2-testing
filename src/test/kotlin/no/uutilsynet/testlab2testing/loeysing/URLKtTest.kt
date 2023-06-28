package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class URLKtTest {
  @Nested
  @DisplayName("sameURL spec")
  inner class SameURLSpec {
    @Test
    @DisplayName("når to URLer har samme host, path og query, så skal de være like")
    fun sammeHostPathQuery() {
      val a = URL("https://www.uutilsynet.no/utvalg?namn=UUTilsynet")
      val b = URL("https://www.uutilsynet.no/utvalg?namn=UUTilsynet")
      assertThat(sameURL(a, b)).isTrue()
    }

    @Test
    @DisplayName("når host er forskjellig, så skal de være ulike")
    fun ulikHost() {
      val a = URL("https://www.uutilsynet.no/utvalg?namn=UUTilsynet")
      val b = URL("https://www.digdir.no/utvalg?namn=UUTilsynet")
      assertThat(sameURL(a, b)).isFalse()
    }

    @Test
    @DisplayName("når subdomene er forskjellig, så skal de være ulike")
    fun ulikSubdomene() {
      val a = URL("https://www.uutilsynet.no/utvalg?namn=UUTilsynet")
      val b = URL("https://uutilsynet.no/utvalg?namn=UUTilsynet")
      assertThat(sameURL(a, b)).isFalse()
    }

    @Test
    @DisplayName("når path er forskjellig, så skal de være ulike")
    fun ulikPath() {
      val a = URL("https://www.uutilsynet.no/tilsyn")
      val b = URL("https://www.uutilsynet.no/utvalg")
      assertThat(sameURL(a, b)).isFalse()
    }

    @Test
    @DisplayName("når query er forskjellig, så skal de være ulike")
    fun ulikQuery() {
      val a = URL("https://www.uutilsynet.no/utvalg?namn=UUTilsynet")
      val b = URL("https://www.uutilsynet.no/utvalg?namn=Digdir")
      assertThat(sameURL(a, b)).isFalse()
    }

    @ParameterizedTest
    @ValueSource(strings = ["/utvalg:/utvalg/", "/utvalg/:/utvalg", ":/"])
    @DisplayName("når path er lik bortsett fra siste /, så skal de være like")
    fun pathMedEllerUtenSlash(paths: String) {
      val (pathA, pathB) = paths.split(":")
      val a = URL("https://www.uutilsynet.no$pathA")
      val b = URL("https://www.uutilsynet.no$pathB")
      assertThat(sameURL(a, b)).isTrue()
    }

    @Test
    @DisplayName(
        "når adressen inneholder en '?', og ikke har query, så skal den ignoreres, og URLene skal være like")
    fun queryIgnoreres() {
      val a = URL("https://www.uutilsynet.no/utvalg?")
      val b = URL("https://www.uutilsynet.no/utvalg")
      assertThat(sameURL(a, b)).isTrue()
    }
  }
}
