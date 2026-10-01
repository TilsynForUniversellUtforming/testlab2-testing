package no.uutilsynet.testlab2testing.security

import jakarta.servlet.http.HttpServletRequest
import no.uutilsynet.testlab2securitylib.ApiKeyAuthenticationProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.stereotype.Service

@Service
class TokenAuthenticationService(val properties: ApiKeyAuthenticationProperties) {
  private val AUTH_TOKEN_HEADER_NAME = "X-API-KEY"

  fun getAuthentication(request: HttpServletRequest): Authentication {
    val apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME)
    if (apiKey == null || apiKey != properties.token) {
      throw BadCredentialsException("Invalid API Key")
    }

    return ApiKeyAuthentication(apiKey, AuthorityUtils.createAuthorityList("brukar subscriber"))
  }
}

@ConfigurationProperties(prefix = "api")
data class ApiKeyAuthenticationPropertiesImpl(override val token: String): ApiKeyAuthenticationProperties {
  override val headerName: String = "X-API-KEY"
}
