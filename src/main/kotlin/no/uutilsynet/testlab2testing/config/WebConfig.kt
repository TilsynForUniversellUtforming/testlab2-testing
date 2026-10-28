package no.uutilsynet.testlab2testing.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
  override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
    configurer.defaultContentType(MediaType.APPLICATION_JSON)
  }


  override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
    builder.configureMessageConverters { converter ->
      if (converter is JacksonJsonHttpMessageConverter) {
        converter.supportedMediaTypes = listOf(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM)
      }
    }
  }
}
