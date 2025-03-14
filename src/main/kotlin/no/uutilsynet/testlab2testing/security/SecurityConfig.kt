package no.uutilsynet.testlab2testing.security

import no.uutilsynet.testlab2securitylib.Testlab2AuthenticationConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

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
        authorize("/ekstern/**", permitAll)
        authorize(anyRequest, hasAuthority("brukar subscriber"))
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
