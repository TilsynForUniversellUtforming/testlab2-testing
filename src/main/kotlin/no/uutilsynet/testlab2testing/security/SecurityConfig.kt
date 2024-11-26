package no.uutilsynet.testlab2testing.security

import no.uutilsynet.testlab2securitylib.ApiKeyAuthenticationProperties
import no.uutilsynet.testlab2securitylib.Testlab2AuthenticationConverter
import no.uutilsynet.testlab2securitylib.apitoken.AuthenticationFilter
import no.uutilsynet.testlab2securitylib.apitoken.TokenAuthenticationService
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain

@Configuration("testingSecurityConfig")
@EnableWebSecurity
class SecurityConfig {

  @Bean("testingSecurityFilterChain")
  @Profile("security")
  fun filterChain(
      http: HttpSecurity,
      authenticationFilter: AuthenticationFilter
  ): SecurityFilterChain {

    http {
      authorizeHttpRequests { authorize(anyRequest, hasAuthority("brukar subscriber")) }
      oauth2ResourceServer {
        jwt { jwtAuthenticationConverter = Testlab2AuthenticationConverter() }
      }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      csrf { disable() }
      addFilterBefore<OAuth2LoginAuthenticationFilter>(authenticationFilter)
    }

    return http.build()
  }

  @Bean
  fun authenticationFilter(
      apiKeyAuthenticationProperties: ApiKeyAuthenticationProperties
  ): AuthenticationFilter {
    val tokenAuthenticationService = TokenAuthenticationService(apiKeyAuthenticationProperties)
    return AuthenticationFilter(tokenAuthenticationService)
  }
}

@ConfigurationProperties(prefix = "api")
class ApiKeyAuthenticationPropertiesImpl(
    override val token: String,
    override val headerName: String = "X-API-KEY"
) : ApiKeyAuthenticationProperties
