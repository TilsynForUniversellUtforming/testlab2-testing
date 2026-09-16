package no.uutilsynet.testlab2testing.kontroll

import io.swagger.v3.oas.annotations.Hidden
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Sakstype
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.NyttTestgrunnlagFromKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestStatus
import no.uutilsynet.testlab2testing.kontroll.delete.KontrollCleanupService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/kontroller")
class KontrollResource(
    private val kontrollDAO: KontrollDAO,
    private val kontrollService: KontrollService,
    private val maalingService: MaalingService,
    private val testgrunnlagService: TestgrunnlagService,
    private val kontrollCleanupService: KontrollCleanupService
) {
    private val logger: Logger = LoggerFactory.getLogger(KontrollResource::class.java)


    @GetMapping
    fun getKontroller(): List<KontrollListItem> {
        return kontrollService.getKontroller()
            .getOrElse {
                logger.error("Feilet henting av  alle kontroller", it)
                throw IllegalStateException(it)
            }
    }

    @PostMapping
    fun createKontroll(@RequestBody opprettKontroll: OpprettKontroll): ResponseEntity<Unit> {
        return runCatching {
            val id = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()
            if (opprettKontroll.kontrolltype == Kontrolltype.ForenklaKontroll) {
                maalingService.nyMaaling(id, opprettKontroll).getOrThrow()
            }

            location(id)
        }
            .fold(
                onSuccess = { location -> ResponseEntity.created(location).build() },
                onFailure = {
                    logger.error("Feil ved oppretting av kontroll", it)
                    ResponseEntity.badRequest().build()
                })
    }

    @GetMapping("/{id}")
    fun getKontroll(@PathVariable id: Int): ResponseEntity<Kontroll> {
        return kontrollService.getKontrollAsResult(id)
            .fold(
                onSuccess = { ResponseEntity.ok(it) },
                onFailure = {
                    when (it) {
                        is IllegalArgumentException -> ResponseEntity.notFound().build()
                        else -> {
                            logger.error("Feil ved henting av kontroll", it)
                            ResponseEntity.internalServerError().build()
                        }
                    }
                })
    }

    @DeleteMapping("/{id}")
    fun deleteKontroll(@PathVariable id: Int): ResponseEntity<Unit> {
        return runCatching {
            kontrollDAO.deleteKontroll(id).getOrThrow()
            maalingService.deleteKontrollMaaling(id).getOrThrow()
        }
            .fold(
                onSuccess = { ResponseEntity.noContent().build() },
                onFailure = {
                    logger.error("Feil ved sletting av kontroll", it)
                    ResponseEntity.internalServerError().build()
                })
    }

    @PutMapping("/{id}")
    fun updateKontroll(
        @PathVariable id: Int,
        @RequestBody updateBody: KontrollUpdate
    ): ResponseEntity<Unit> =
        runCatching {
            require(updateBody.kontroll.id == id) { "id i URL-en og id er ikkje den same" }

            when (updateBody) {
                is KontrollUpdate.Edit -> {
                    kontrollDAO.updateKontroll(updateBody.kontroll)
                }

                is KontrollUpdate.Utval -> {
                    val (kontroll, utvalId) = updateBody
                    kontrollDAO.updateKontroll(kontroll, utvalId).getOrThrow()
                }

                is KontrollUpdate.Testreglar -> {
                    val (kontroll, testreglar) = updateBody
                    val (regelsettId, testregelIdList) = testreglar
                    kontrollDAO.updateKontroll(kontroll, regelsettId, testregelIdList).getOrThrow()
                }

                is KontrollUpdate.Sideutval -> {
                    val (kontroll, sideutvalList) = updateBody
                    if (sideutvalList.any { it.begrunnelse.isBlank() }) {
                        logger.error("Ugyldig sideutval for kontroll: ${kontroll.id}")
                        throw IllegalArgumentException("Ugyldige sider i sideutval")
                    }
                    kontrollDAO.updateKontroll(kontroll, sideutvalList).getOrThrow()
                }
            }
            if (updateBody.kontroll.kontrolltype == Kontrolltype.ForenklaKontroll) {
                maalingService.updateMaaling(kontrollService.getKontrollAsResult(id).getOrThrow())
            } else {
                createOrUpdateTestgrunnlag(id)
            }
        }
            .fold(
                onSuccess = { ResponseEntity.noContent().build() },
                onFailure = {
                    when (it) {
                        is IllegalArgumentException -> ResponseEntity.badRequest().build()
                        else -> {
                            logger.error("Feil ved oppdatering av kontroll", it)
                            ResponseEntity.internalServerError().build()
                        }
                    }
                })

    @GetMapping("sideutvaltype")
    fun getSideutvalType(): ResponseEntity<out Any> =
        runCatching { ResponseEntity.ok(kontrollDAO.getSideutvalType()) }
            .getOrElse {
                logger.error("Feila ved henting av sideutvaltyper", it)
                ResponseEntity.internalServerError().body(it.message)
            }

    @GetMapping("/test-status/{kontrollId}")
    fun getTestStatus(
        @PathVariable kontrollId: Int,
    ): ResponseEntity<TestStatus> {
        val hasTestresultat = testgrunnlagService.kontrollHasTestresultat(kontrollId)
        return ResponseEntity.ok(if (hasTestresultat) TestStatus.Started else TestStatus.Pending)
    }

    @Hidden
    @PreAuthorize("hasRole('brukar_editor')")
    @DeleteMapping("/id/{kontrollId}")
    fun deleteKontrollData(
        @PathVariable kontrollId: Int,
    ): ResponseEntity<Unit> {
        return runCatching {
            kontrollCleanupService.cleanupKontrollData(kontrollId)
        }.fold(
            onSuccess = { ResponseEntity.noContent().build() },
            onFailure = {
                logger.error("Feil ved sletting av testresultat for kontroll $kontrollId", it)
                ResponseEntity.internalServerError().build()
            }
        )
    }


    @GetMapping("/testmetadata/{kontrollId}")
    fun testingMetadata(@PathVariable kontrollId: Int): KontrollTestingMetadata {
        return kontrollService.testingMetadata(kontrollId)
    }

    fun createOrUpdateTestgrunnlag(kontrollId: Int): Result<Int> {
        val kontroll = kontrollService.getKontrollAsResult(kontrollId).getOrThrow()

        val nyttTestgrunnlag =
            NyttTestgrunnlagFromKontroll(
                kontroll.id,
                "Testgrunnlag for kontroll ${kontroll.tittel}",
                OPPRINNELEG_TEST,
                kontroll.sideutvalList,
                kontroll.testreglar?.testregelIdList ?: emptyList()
            )
        return testgrunnlagService.createOrUpdateFromKontroll(nyttTestgrunnlag)
    }


    private fun location(id: Int) =
        ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

    data class KontrollListItem(
        val id: Int,
        val tittel: String,
        val saksbehandler: String,
        val sakstype: Sakstype,
        val arkivreferanse: String,
        val kontrolltype: Kontrolltype,
        val virksomheter: List<String>, // liste med orgnummer
        val styringsdataId: Int?
    )

    data class OpprettKontroll(
        val tittel: String,
        val saksbehandler: String,
        val sakstype: Sakstype,
        val arkivreferanse: String,
        val kontrolltype: Kontrolltype,
    )

}
