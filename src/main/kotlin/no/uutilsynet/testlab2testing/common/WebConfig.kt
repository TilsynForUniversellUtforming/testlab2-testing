package no.uutilsynet.testlab2testing.common

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
  override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
    configurer.defaultContentType(MediaType.APPLICATION_JSON)
  }
}
