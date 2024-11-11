package no.uutilsynet.testlab2testing

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.client.RestTemplate

@Configuration
@EnableWebSecurity
@Profile("test")
class ITTestSecurityConfig {

  @Bean
  fun testFilterChain(http: HttpSecurity): SecurityFilterChain {

    http {
      authorizeHttpRequests { authorize(anyRequest, permitAll) }
      csrf { disable() }
    }

    return http.build()
  }

  @Bean
  fun restTemplate(
      restTemplateBuilder: RestTemplateBuilder,
  ): RestTemplate {
    val objectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val mappingJackson2HttpMessageConverter = MappingJackson2HttpMessageConverter()
    mappingJackson2HttpMessageConverter.objectMapper = objectMapper
    mappingJackson2HttpMessageConverter.supportedMediaTypes =
        listOf(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM)

    return restTemplateBuilder.messageConverters(mappingJackson2HttpMessageConverter).build()
  }
}
