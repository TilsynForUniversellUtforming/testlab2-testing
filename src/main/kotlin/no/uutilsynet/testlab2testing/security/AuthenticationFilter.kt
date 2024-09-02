package no.uutilsynet.testlab2testing.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean

@Component
class AuthenticationFilter(val tokenAuthenticationService: TokenAuthenticationService) :
    GenericFilterBean() {

  private val AUTH_TOKEN_HEADER_NAME = "X-API-KEY"

  @Throws(IOException::class, ServletException::class)
  override fun doFilter(
      request: ServletRequest,
      response: ServletResponse,
      filterChain: FilterChain
  ) {
    try {
      val authentication: Authentication =
          tokenAuthenticationService.getAuthentication(request as HttpServletRequest)
      SecurityContextHolder.getContext().authentication = authentication
    } catch (exp: Exception) {
      val httpResponse = response as HttpServletResponse
      httpResponse.status = HttpServletResponse.SC_UNAUTHORIZED
      httpResponse.contentType = MediaType.APPLICATION_JSON_VALUE
      val writer = httpResponse.writer
      writer.print(exp.message)
      writer.flush()
      writer.close()
    }

    filterChain.doFilter(request, response)
  }
}
