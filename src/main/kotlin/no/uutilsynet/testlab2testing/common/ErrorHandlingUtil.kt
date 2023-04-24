package no.uutilsynet.testlab2testing.common

import java.net.URI
import org.springframework.http.ResponseEntity

object ErrorHandlingUtil {
  fun handleErrors(exception: Throwable): ResponseEntity<Any> =
      when (exception) {
        is NullPointerException -> ResponseEntity.badRequest().body(exception.message)
        else -> ResponseEntity.internalServerError().body(exception.message)
      }

  fun executeWithErrorHandling(func: () -> Int): ResponseEntity<out Any> =
      runCatching { ResponseEntity.ok(func()) }.getOrElse { ex -> handleErrors(ex) }

  fun createWithErrorHandling(
      func: () -> Int,
      locationForId: (id: Int) -> URI
  ): ResponseEntity<out Any> =
      runCatching { func() }
          .fold(
              { id ->
                val location = locationForId(id)
                ResponseEntity.created(location).build()
              },
              { exception -> handleErrors(exception) })
}
