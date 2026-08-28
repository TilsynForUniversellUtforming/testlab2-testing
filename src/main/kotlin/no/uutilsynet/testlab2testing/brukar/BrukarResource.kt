package no.uutilsynet.testlab2testing.brukar

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class BrukarResource(val brukarService: BrukarService) {

    @GetMapping()
    fun listBrukarar(): List<Brukar> {
        return brukarService.getBrukarList()
    }
}