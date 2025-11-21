package no.uutilsynet.testlab2testing.forenkletkontroll.dto

import com.fasterxml.jackson.annotation.JsonInclude
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EditMaalingDTO(
    val id: Int,
    val navn: String,
    val loeysingIdList: List<Int>?,
    val testregelIdList: List<Int>?,
    val crawlParameters: CrawlParameters?
)
