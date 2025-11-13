package no.uutilsynet.testlab2testing.ekstern.resultat.paginering

import no.uutilsynet.testlab2testing.ekstern.resultat.model.TestresultatDetaljertEkstern
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class TestresultatEksternAssembler :
    RepresentationModelAssembler<TestresultatDetaljertEkstern, TestresultatDetaljertEkstern> {
    override fun toModel(entity: TestresultatDetaljertEkstern): TestresultatDetaljertEkstern {
        return entity
    }
}
