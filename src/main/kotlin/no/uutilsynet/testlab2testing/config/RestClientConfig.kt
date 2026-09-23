package no.uutilsynet.testlab2testing.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {
  @Bean
  fun restClient(
      builder: RestClient.Builder,
      jacksonJsonHttpMessageConverter: JacksonJsonHttpMessageConverter,
  ): RestClient {
    return builder
        .messageConverters { converters ->
          converters.removeIf { it is JacksonJsonHttpMessageConverter }
          converters.add(0, jacksonJsonHttpMessageConverter)
        }
        .bufferContent({ uri, method -> true })
        .requestInterceptor({ request, body, execution -> execution.execute(request, body) })
        .build()
  }
}
