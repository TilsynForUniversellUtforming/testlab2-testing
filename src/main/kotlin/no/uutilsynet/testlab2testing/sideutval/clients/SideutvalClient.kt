package no.uutilsynet.testlab2testing.sideutval.clients

import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import no.uutilsynet.testlab2testing.kontroll.SideutvalType
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterProperties
import no.uutilsynet.testlab2testing.testregel.krav.KravRegisterProperties
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class SideutvalClient(private val clientsProperties: LoeysingsRegisterProperties, builder: RestClient.Builder) {
    private val restClient: RestClient = builder
        .baseUrl(clientsProperties.host)
        .build()

    private val sideutvalUrl: String = clientsProperties.host + SIDEUTVALPATH


    fun lookupLoeysing(loeysingId: Int): Map<Int,String> {
        return restClient.get()
            .uri(
                this.sideutvalUrl + "/loeysing/" + loeysingId
            )
            .retrieve()
            .body<Array<SideutvalResponse>>()?.associate {
                it.id to it.side
            } ?: emptyMap()
    }



    companion object {
        private const val SIDEUTVALPATH = "/v1/sideutval"
    }
}

data class SideutvalResponse(
    val type: SideutvalType?,
    val sidetype: Sidetype,
    val side: String,
    val id: Int,
)

enum class Sidetype {
    NETTSIDE,
    APPSIDE
}
