package no.uutilsynet.testlab2testing.brukar

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class BrukarService(val brukarDAO: BrukarDAO) {

  @Throws(RuntimeException::class)
  fun getCurrentUser(): Brukar {
    val authentication: Authentication = SecurityContextHolder.getContext().authentication
    if (authentication !is AnonymousAuthenticationToken) {
      val principal = authentication.principal as Jwt
      principal.getClaimAsString("preferred_username")
      val username = principal.getClaimAsString("preferred_username")
      val fullName = principal.getClaimAsString("name")

      return Brukar(username, fullName)
    }
    throw RuntimeException("No authenticated user")
  }

  fun saveIfNotExists(brukar: Brukar): Int {
    return brukarDAO.getBrukarId(brukar.brukarnamn) ?: brukarDAO.saveBrukar(brukar)
  }

  fun getUserId(): Int? {
    val brukar = getCurrentUser()
    return saveIfNotExists(brukar)
  }
}
