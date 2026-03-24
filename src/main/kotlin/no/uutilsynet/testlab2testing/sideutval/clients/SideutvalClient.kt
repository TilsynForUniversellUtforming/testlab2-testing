package no.uutilsynet.testlab2testing.sideutval.clients

import no.uutilsynet.testlab2testing.testregel.krav.KravRegisterProperties
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class SideutvalClient(private val clientsProperties: KravRegisterProperties, builder: RestClient.Builder) {
    private val restClient: RestClient

    private val sideutvalUrl: String

    init {
        this.restClient = builder
            .baseUrl(clientsProperties.host)
            .build()
        this.sideutvalUrl = clientsProperties.host + SIDEUTVALPATH
    }



    fun lookupLoeysing(loeysingId: Int): Map<Int,String> {
        return mapOf(1 to "https://loeysing1.no", 2 to "https://loeysing2.no") as Map<Int, String>

     /*   return restClient.get()
            .uri(
                this.sideutvalUrl + "/loeysing/loeysingId/" + loeysingId
            )
            .retrieve()
            .body<Array<String>>()  */
    }



    companion object {
        private const val SIDEUTVALPATH = "/v1/sideutval"
    }
}