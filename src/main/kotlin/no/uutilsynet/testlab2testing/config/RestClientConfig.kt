package no.uutilsynet.testlab2testing.config

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.datatype.jsr310.JavaTimeModule
import tools.jackson.module.kotlin.KotlinModule

@Configuration
class RestClientConfig {
  @Bean
  fun restClient(
      builder: RestClient.Builder,
      jacksonJsonHttpMessageConverter: JacksonJsonHttpMessageConverter,
  ): RestClient {
    return builder
        .configureMessageConverters { builder ->
          builder.registerDefaults()
          builder.addCustomConverter(jacksonJsonHttpMessageConverter)
        }
        .bufferContent({ _, _ -> true })
        .requestInterceptor({ request, body, execution -> execution.execute(request, body) })
        .build()
  }

  @Bean
  fun jsonMapper(): JsonMapper {
    return JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .addModule(JavaTimeModule())
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
