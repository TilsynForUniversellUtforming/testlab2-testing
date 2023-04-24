package no.uutilsynet.testlab2testing.common

import java.net.URI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ErrorHandlingUtilTests {

  private val errorMessage = "test message"
  private val funcId = 1
  private val okFunction = { funcId }
  private val npeFunction = { throw NullPointerException(errorMessage) }
  private val otherExFunction = { throw Exception(errorMessage) }
  private val locationForId = { id: Int -> URI.create("/test/$id") }

  @Test
  @DisplayName("handleErrors skal returnere 'bad request' for NullPointerException")
  fun handleErrorsNPE() {
    val responseEntity = ErrorHandlingUtil.handleErrors(NullPointerException(errorMessage))
    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.statusCode)
    assertEquals(errorMessage, responseEntity.body)
  }

  @Test
  @DisplayName("handleErrors skal returnere 'internal server error' for andre exceptions")
  fun handleErrorsOtherException() {
    val responseEntity = ErrorHandlingUtil.handleErrors(Exception(errorMessage))
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.statusCode)
    assertEquals(errorMessage, responseEntity.body)
  }

  @Test
  @DisplayName("executeWithErrorHandling skal returnere 'ok' response hvis funksjonen går gjennom")
  fun executeWithErrorHandlingSuccess() {
    val responseEntity = ErrorHandlingUtil.executeWithErrorHandling(okFunction)
    assertEquals(HttpStatus.OK, responseEntity.statusCode)
    assertEquals(1, responseEntity.body)
  }

  @Test
  @DisplayName("executeWithErrorHandling skal returnere 'bad request' hvis funksjonen kaster NPE")
  fun executeWithErrorHandlingNPE() {
    val responseEntity = ErrorHandlingUtil.executeWithErrorHandling(npeFunction)
    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.statusCode)
    assertEquals(errorMessage, responseEntity.body)
  }

  @Test
  @DisplayName(
      "executeWithErrorHandling skal returnere 'internal server error' hvis funksjonen kaster andre exceptions")
  fun executeWithErrorHandlingOtherException() {
    val responseEntity = ErrorHandlingUtil.executeWithErrorHandling(otherExFunction)
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.statusCode)
    assertEquals(errorMessage, responseEntity.body)
  }

  @Test
  @DisplayName(
      "executeWithErrorHandling skal returnere 'created' response hvis funksjonen går gjennom")
  fun createWithErrorHandlingSuccess() {
    val responseEntity = ErrorHandlingUtil.createWithErrorHandling(okFunction, locationForId)
    assertEquals(HttpStatus.CREATED, responseEntity.statusCode)
    assertEquals(URI.create("/test/$funcId"), responseEntity.headers.location)
  }

  @Test
  @DisplayName("createWithErrorHandling skal returnere 'bad request' hvis funksjonen kaster NPE")
  fun createWithErrorHandlingNPE() {
    val responseEntity = ErrorHandlingUtil.createWithErrorHandling(npeFunction, locationForId)
    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.statusCode)
    assertEquals(errorMessage, responseEntity.body)
  }

  @Test
  @DisplayName(
      "createWithErrorHandling skal returnere 'internal server error' hvis funksjonen kaster andre exceptions")
  fun createWithErrorHandlingOtherException() {
    val responseEntity = ErrorHandlingUtil.createWithErrorHandling(otherExFunction, locationForId)
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.statusCode)
    assertEquals(errorMessage, responseEntity.body)
  }
}
