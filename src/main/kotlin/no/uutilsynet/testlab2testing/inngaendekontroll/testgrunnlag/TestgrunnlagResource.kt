package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/testgrunnlag")
class TestgrunnlagResource(val testgrunnlagDAO: TestgrunnlagDAO) {

    @GetMapping
    fun getTestgrunnlagList(sakId:Int,loeysingId:Int?): ResponseEntity<List<Testgrunnlag>> {
        return ResponseEntity.ok(testgrunnlagDAO.getTestgrunnlagForSak(sakId,loeysingId))
    }
}