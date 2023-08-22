package no.uutilsynet.testlab2testing.common

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@TestConfiguration
class TestSecurityConfig {

  @Bean()
  open fun resouceServerFilterChain(http: HttpSecurity): SecurityFilterChain {

    http {
      authorizeHttpRequests { authorize(anyRequest, permitAll) }
      csrf { disable() }
    }

    return http.build()
  }
}
