package no.uutilsynet.testlab2testing

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.uutilsynet.testlab2securitylib.interceptor.ApiTokenInterceptor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.CommonsRequestLoggingFilter

@SpringBootApplication(
    exclude = [SecurityAutoConfiguration::class],
    scanBasePackages = ["no.uutilsynet.testlab2testing", "no.uutilsynet.testlab2securitylib"])
@ConfigurationPropertiesScan
@EnableScheduling
@EnableCaching
class Testlab2TestingApplication {

  @Bean
  @Profile("security")
  fun restTemplateSecurity(
      restTemplateBuilder: RestTemplateBuilder,
      apiTokenInterceptor: ApiTokenInterceptor
  ): RestTemplate {
    val mappingJackson2HttpMessageConverter = mappingJackson2HttpMessageConverter()
    val interceptors: ArrayList<ClientHttpRequestInterceptor> = ArrayList()
    interceptors.add(apiTokenInterceptor)

    return restTemplateBuilder
        .messageConverters(mappingJackson2HttpMessageConverter)
        .requestFactory(::reqestFactory)
        .interceptors(interceptors)
        .build()
  }

  @Bean
  @Profile("!security")
  fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
    val mappingJackson2HttpMessageConverter = mappingJackson2HttpMessageConverter()
    return restTemplateBuilder
        .messageConverters(mappingJackson2HttpMessageConverter)
        .requestFactory(::reqestFactory)
        .build()
  }

  private fun mappingJackson2HttpMessageConverter(): MappingJackson2HttpMessageConverter {
    val objectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val mappingJackson2HttpMessageConverter = MappingJackson2HttpMessageConverter()
    mappingJackson2HttpMessageConverter.objectMapper = objectMapper
    mappingJackson2HttpMessageConverter.supportedMediaTypes =
        listOf(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM)
    return mappingJackson2HttpMessageConverter
  }

  @Bean
  fun commonsRequestLoggingFilter(): CommonsRequestLoggingFilter {
    val filter = CommonsRequestLoggingFilter()
    filter.setIncludeQueryString(true)
    filter.setIncludePayload(true)
    filter.setMaxPayloadLength(1000)
    return filter
  }

  @Bean
  fun reqestFactory(): BufferingClientHttpRequestFactory {
    return BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
  }
}

fun main(args: Array<String>) {
  runApplication<Testlab2TestingApplication>(*args)
}
