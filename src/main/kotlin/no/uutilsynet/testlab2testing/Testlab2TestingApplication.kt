package no.uutilsynet.testlab2testing

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate

@SpringBootApplication
@ConfigurationPropertiesScan
class Testlab2TestingApplication {
  @Bean
  fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
    val objectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val mappingJackson2HttpMessageConverter = MappingJackson2HttpMessageConverter()
    mappingJackson2HttpMessageConverter.objectMapper = objectMapper
    return restTemplateBuilder.messageConverters(mappingJackson2HttpMessageConverter).build()
  }
}

fun main(args: Array<String>) {
  runApplication<Testlab2TestingApplication>(*args)
}
