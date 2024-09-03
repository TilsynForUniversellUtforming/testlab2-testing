package no.uutilsynet.testlab2testing.security

import java.security.SecureRandom
import java.util.*

/** Utils til å generere token */
class TokenHelper {

  fun generateSecureRandom(): String {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    val random = SecureRandom()
    val buffer = ByteArray(20)
    random.nextBytes(buffer)
    return encoder.encodeToString(buffer)
  }
}
