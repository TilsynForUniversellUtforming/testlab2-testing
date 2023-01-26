package no.uutilsynet.testlab2testing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication @ConfigurationPropertiesScan class Testlab2TestingApplication

fun main(args: Array<String>) {
  runApplication<Testlab2TestingApplication>(*args)
}
