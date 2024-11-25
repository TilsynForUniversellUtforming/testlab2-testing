package no.uutilsynet.testlab2testing

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@TestConfiguration
@Qualifier("testconfiguration")
class Testlab2TestingTestApplication {}
