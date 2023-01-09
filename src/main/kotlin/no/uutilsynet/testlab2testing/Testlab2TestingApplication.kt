package no.uutilsynet.testlab2testing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class Testlab2TestingApplication

fun main(args: Array<String>) {
	runApplication<Testlab2TestingApplication>(*args)
}
