package no.uutilsynet.testlab2testing

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AppNameResource {
    @GetMapping("/app-name")
    fun appName():String = "testlab2-testing"
}