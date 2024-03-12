package no.uutilsynet.testlab2testing

import no.uutilsynet.testlab2securitylib.Testlab2AuthenticationConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig {
  @Bean
  @Profile("security")
  open fun filterChain(http: HttpSecurity): SecurityFilterChain {

    http {
      authorizeHttpRequests { authorize(anyRequest, hasAuthority("brukar subscriber")) }
      oauth2ResourceServer {
        jwt { jwtAuthenticationConverter = Testlab2AuthenticationConverter() }
      }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      csrf { disable() } //
    }

    return http.build()
  }

  @Bean
  @Profile("!security")
  fun openFilterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      authorizeHttpRequests { authorize(anyRequest, permitAll) }
      cors {}
    }
    return http.build()
  }

  @Bean
  fun corsConfigurationSource(): CorsConfigurationSource {
    val configuration = CorsConfiguration()
    configuration.allowedOrigins =
        listOf(
            "https://user.difi.no",
            "https://test-testlab.uutilsynet.no",
            "https://beta-testlab.uutilsynet.no",
            "https://user.difi.no")
    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", configuration)
    return source
  }
}
