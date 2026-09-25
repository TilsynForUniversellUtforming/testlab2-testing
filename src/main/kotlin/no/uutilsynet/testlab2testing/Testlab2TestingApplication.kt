package no.uutilsynet.testlab2testing

import jakarta.validation.ClockProvider
import java.time.Clock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.filter.CommonsRequestLoggingFilter

@SpringBootApplication(
    exclude = [SecurityAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
@ConfigurationPropertiesScan
@EnableScheduling
class Testlab2TestingApplication {

  @Bean
  fun commonsRequestLoggingFilter(): CommonsRequestLoggingFilter {
    val filter = CommonsRequestLoggingFilter()
    filter.setIncludeQueryString(true)
    filter.setIncludePayload(true)
    filter.setMaxPayloadLength(1000)
    return filter
  }

  @Bean
  fun clockProvider(): ClockProvider {
    return ClockProvider { Clock.systemDefaultZone() }
  }
}

fun main(args: Array<String>) {
  runApplication<Testlab2TestingApplication>(*args)
}
