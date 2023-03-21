package no.uutilsynet.testlab2testing.test

import no.uutilsynet.testlab2testing.maaling.AutoTesterClient
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/tester")
class TestResource(val maalingDAO: MaalingDAO, val autoTesterClient: AutoTesterClient)
