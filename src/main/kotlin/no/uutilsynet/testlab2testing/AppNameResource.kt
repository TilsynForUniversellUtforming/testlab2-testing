package no.uutilsynet.testlab2testing

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class AppNameResource {
  @GetMapping("/") fun appName(): AppName = AppName("testlab2-testing")

  @GetMapping("/testHeaders")
  fun appheaders(@RequestHeader allHeaders: Map<String, String>): String {
    println("Headers " + allHeaders)
    return allHeaders.toString()
  }

  data class AppName(val appName: String)
}
