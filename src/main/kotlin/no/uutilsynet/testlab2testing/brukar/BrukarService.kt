package no.uutilsynet.testlab2testing.brukar

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class BrukarService(val brukarDAO: BrukarDAO) {

  fun getCurrentUser(): Brukar {
    val authentication = SecurityContextHolder.getContext().authentication
    if (authentication != null && authentication !is AnonymousAuthenticationToken) {
      val principal = authentication.principal as Jwt
      principal.getClaimAsString("preferred_username")
      val username = principal.getClaimAsString("preferred_username")
      val fullName = principal.getClaimAsString("name")

      requireNotNull(username) { "preferred_username claim is missing in JWT" }
      requireNotNull(fullName) { "preferred_fullname claim is missing in JWT" }

      return Brukar(username, fullName)
    }
    return Brukar("anonym", "anonymous")
  }

  fun getUserId(brukar: Brukar): Int {
    return brukarDAO.getBrukarId(brukar.brukarnamn) ?: brukarDAO.saveBrukar(brukar)
  }

  fun getUserId(): Int? {
    val brukar = getCurrentUser()
    return getUserId(brukar)
  }

  fun getBrukarById(brukarId: Int): Brukar? {
    return brukarDAO.getBrukarById(brukarId)
  }
}
