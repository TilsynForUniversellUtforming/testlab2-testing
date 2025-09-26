package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EksternResultatPubliseringService(
    @Autowired val kontrollDAO: KontrollDAO,
    @Autowired val eksternResultatDAO: EksternResultatDAO,
    @Autowired val testgrunnlagDAO: TestgrunnlagDAO,
    @Autowired val maalingService: MaalingService,
    @Autowired val cacheManager: CacheManager
) {
  private val logger = LoggerFactory.getLogger(EksternResultatPubliseringService::class.java)

  @Transactional
  fun publiser(kontrollId: Int) {
    runCatching {
          val kontroll = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
          val erPublisert =
              eksternResultatDAO.erKontrollPublisert(kontrollId, kontroll.kontrolltype)

          updateCache("resultatKontroll", kontrollId)

          return if (erPublisert) {
            avpubliserResultat(kontroll.id)
          } else {
            publiserResultat(kontroll.id)
          }
        }
        .onFailure {
          logger.error("Feil ved publisering av resultat", it)
          throw it
        }
  }

  fun publiserResultat(kontrollId: Int) {
    runCatching {
          val kontroll = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
          val rapportId = eksternResultatDAO.getRapportIdForKontroll(kontroll.id).getOrThrow()
          logger.info("Publiserer resultat for kontroll med id $kontrollId")
          getLoeysingarForKontroll(kontroll).forEach {
            publiserResultatForLoeysingForKontroll(kontroll, it.id, rapportId)
          }
        }
        .onFailure {
          logger.error("Feil ved publisering av resultat", it)
          throw it
        }
  }

  fun avpubliserResultat(kontrollId: Int) {
    runCatching {
          val kontroll = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
          logger.info("Avpubliserer resultat for kontroll med id $kontrollId")
          getLoeysingarForKontroll(kontroll).forEach {
            avpubliserResultatForLoeysingForKontroll(kontroll, it.id).getOrThrow()
          }
        }
        .onFailure {
          logger.error("Feil ved avpublisering av resultat", it)
          throw it
        }
  }

  fun getLoeysingarForKontroll(
      kontroll: KontrollDAO.KontrollDB
  ): List<KontrollDAO.KontrollDB.Loeysing> {
    require(!(kontroll.utval == null || kontroll.utval.loeysingar.isEmpty())) {
      "Kontrollen har ingen loeysingar"
    }
    return kontroll.utval.loeysingar
  }

  private fun publiserResultatForLoeysingForKontroll(
      kontroll: KontrollDAO.KontrollDB,
      loeysingId: Int,
      rapportId: String?
  ): Result<Boolean> {
    return when (kontroll.kontrolltype) {
      Kontrolltype.Statusmaaling,
      Kontrolltype.ForenklaKontroll ->
          publiserResultatForMaaling(kontroll.id, loeysingId, rapportId)
      Kontrolltype.Tilsyn,
      Kontrolltype.Uttalesak,
      Kontrolltype.InngaaendeKontroll ->
          publiserResultatForTestgrunnlag(kontroll.id, loeysingId, rapportId)
    }
  }

  private fun publiserResultatForTestgrunnlag(
      kontrollId: Int,
      loeysingId: Int,
      rapportId: String?
  ): Result<Boolean> {
    val testgrunnlagId = testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).opprinneligTest.id
    return eksternResultatDAO.publisertTestgrunnlagResultat(testgrunnlagId, loeysingId, rapportId)
  }

  private fun publiserResultatForMaaling(
      kontrollId: Int,
      loeysingId: Int,
      rapportId: String?
  ): Result<Boolean> {
    val maalingId = maalingService.getMaalingForKontroll(kontrollId)
    return eksternResultatDAO.publiserMaalingResultat(maalingId, loeysingId, rapportId)
  }

  private fun avpubliserResultatForLoeysingForKontroll(
      kontroll: KontrollDAO.KontrollDB,
      loeysingId: Int
  ): Result<Boolean> {
    return when (kontroll.kontrolltype) {
      Kontrolltype.Statusmaaling,
      Kontrolltype.ForenklaKontroll -> avpubliserResultatForMaaling(kontroll.id, loeysingId)
      Kontrolltype.Tilsyn,
      Kontrolltype.Uttalesak,
      Kontrolltype.InngaaendeKontroll -> avpubliserResultatForTestgrunnlag(kontroll.id, loeysingId)
    }
  }

  private fun avpubliserResultatForTestgrunnlag(kontrollId: Int, loeysingId: Int): Result<Boolean> {
    val testgrunnlagId = testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).opprinneligTest.id
    return eksternResultatDAO.avpubliserResultatTestgrunnlag(testgrunnlagId, loeysingId)
  }

  private fun avpubliserResultatForMaaling(kontrollId: Int, loeysingId: Int): Result<Boolean> {
    val maalingId = maalingService.getMaalingForKontroll(kontrollId)
    return eksternResultatDAO.avpubliserResultatMaaling(maalingId, loeysingId)
  }

  private fun updateCache(cache: String, id: Int) {
    cacheManager.getCache(cache)?.evict(id)
  }
}
