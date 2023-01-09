package no.uutilsynet.testlab2testing

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AppNameResource {
  @GetMapping("/") fun appName(): AppName = AppName("testlab2-testing")

  data class AppName(val appName: String)
}
