package no.uutilsynet.testlab2testing.kontroll

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kontrollSteg")
@JsonSubTypes(
    JsonSubTypes.Type(value = KontrollUpdate.Edit::class, name = "edit"),
    JsonSubTypes.Type(value = KontrollUpdate.Utval::class, name = "utval"),
    JsonSubTypes.Type(value = KontrollUpdate.Testreglar::class, name = "testreglar"),
    JsonSubTypes.Type(value = KontrollUpdate.Sideutval::class, name = "sideutval"))
sealed class KontrollUpdate {
  abstract val kontroll: Kontroll

  data class Edit(override val kontroll: Kontroll) : KontrollUpdate()

  data class Utval(override val kontroll: Kontroll, val utvalId: Int) : KontrollUpdate()

  data class Testreglar(override val kontroll: Kontroll, val testreglar: KontrollTestreglarUpdate) :
      KontrollUpdate()

  data class Sideutval(override val kontroll: Kontroll, val sideutvalList: List<SideutvalElementBase>) :
      KontrollUpdate()
}

data class KontrollTestreglarUpdate(
    val regelsettId: Int? = null,
    val testregelIdList: List<Int> = emptyList()
)

enum class KontrollSteg {
  @JsonProperty("edit") Edit,
  @JsonProperty("utval") Utval,
  @JsonProperty("testreglar") Testreglar,
  @JsonProperty("sideutval") Sideutval
}
