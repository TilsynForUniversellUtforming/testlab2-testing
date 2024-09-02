package no.uutilsynet.testlab2testing.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class ApiKeyAuthentication(
    private val apiKey: String,
    authorities: Collection<GrantedAuthority?>?
) : AbstractAuthenticationToken(authorities) {
  init {
    isAuthenticated = true
  }

  override fun getCredentials(): Any? {
    return null
  }

  override fun getPrincipal(): Any {
    return apiKey
  }
}
