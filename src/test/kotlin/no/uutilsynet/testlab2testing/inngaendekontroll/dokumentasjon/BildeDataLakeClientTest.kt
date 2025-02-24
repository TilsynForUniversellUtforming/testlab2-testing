package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest()
class BildeDataLakeClientTest(@Autowired val dataLakeClient: BildeDataLakeClient) {

  @Test
  @Ignore
  fun testUpload() {

    val testdata = "testdatafile"
    val data = dataToBytArrayInputStream(testdata)

    val path = "testdirectory/testfile.txt"

    dataLakeClient.uploadToStorage(data, path).onFailure { println("Error: ${it.message}") }
  }

  private fun dataToBytArrayInputStream(testdata: String): ByteArrayInputStream {
    return ByteArrayOutputStream().use {
      it.write(testdata.toByteArray())
      it.toByteArray()
      ByteArrayInputStream(it.toByteArray())
    }
  }
}
