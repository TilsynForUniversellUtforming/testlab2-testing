package no.uutilsynet.testlab2testing

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@Profile("test")
class ITTestSecurityConfig {

  @Bean
  open fun testFilterChain(http: HttpSecurity): SecurityFilterChain {

    http {
      authorizeHttpRequests { authorize(anyRequest, permitAll) }
      csrf { disable() }
    }

    return http.build()
  }
}
