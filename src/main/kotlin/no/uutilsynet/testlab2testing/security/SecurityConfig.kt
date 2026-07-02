package no.uutilsynet.testlab2testing.security

import no.uutilsynet.testlab2securitylib.Testlab2AuthenticationConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

private const val BRUKAR_SUBSCRIBER = "brukar subscriber"

private const val UTVAL_PATH = "/v1/utval/**"

@Configuration
@EnableWebSecurity
class SecurityConfig {

  @Bean
  @Profile("security")
  open fun filterChain(
      http: HttpSecurity,
      @Autowired authenticationFilter: AuthenticationFilter
  ): SecurityFilterChain {

    http {
      authorizeHttpRequests {
        authorize(HttpMethod.POST, UTVAL_PATH, permitAll)
        authorize(HttpMethod.PUT, UTVAL_PATH, permitAll)
        authorize(HttpMethod.DELETE, UTVAL_PATH, permitAll)
        authorize(HttpMethod.POST, "/**", hasAuthority(BRUKAR_SUBSCRIBER))
        authorize(HttpMethod.PUT, "/**", hasAuthority(BRUKAR_SUBSCRIBER))
        authorize(HttpMethod.DELETE, "/**", hasAuthority(BRUKAR_SUBSCRIBER))
        authorize(anyRequest, permitAll)
      }
      oauth2ResourceServer {
        jwt { jwtAuthenticationConverter = Testlab2AuthenticationConverter() }
      }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      csrf { disable() }
      cors { disable() }
    }

    return http.build()
  }

  @Bean
  @Profile("!security")
  fun openFilterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      authorizeHttpRequests { authorize(anyRequest, permitAll) }
      cors {}
      csrf { disable() }
    }
    return http.build()
  }
}
