package no.uutilsynet.testlab2testing

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "features") data class Features(val startTesting: Boolean)
