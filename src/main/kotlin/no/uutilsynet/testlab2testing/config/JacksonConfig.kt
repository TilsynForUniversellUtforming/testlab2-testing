package no.uutilsynet.testlab2testing.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

@Configuration
class JacksonConfig {

  @Bean
  fun jsonMapper(): JsonMapper {
    return JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()
  }

  @Bean
  fun jacksonJsonHttpMessageConverter(jsonMapper: JsonMapper): JacksonJsonHttpMessageConverter {
    return JacksonJsonHttpMessageConverter(jsonMapper).apply {
      supportedMediaTypes = listOf(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM)
    }
  }
}
