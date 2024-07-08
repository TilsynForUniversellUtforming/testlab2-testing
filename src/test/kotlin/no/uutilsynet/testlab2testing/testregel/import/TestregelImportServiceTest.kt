package no.uutilsynet.testlab2testing.testregel.import

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.nio.charset.Charset
import java.util.*

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class TestregelImportServiceTest(@Autowired val testregelImportService: TestregelImportService) {

  @Test
  fun test() {

    testregelImportService.readFolder()
    println("Test")
  }

  @Test
  fun readTestreglerFolder()
    {
        val testregelFolder = testregelImportService.getTestreglarFolder()

      val folder111 = testregelFolder?.filter { it.name == "1.1.1" }?.first()
      assertThat(folder111).isNotNull
      assertThat(folder111?.name).isEqualTo("1.1.1")

    }

  @Test
  fun readTestregelType() {
    val testregelFolder = testregelImportService.getTestregelTypeFolder("1.1.1")?.first()
    assertThat(testregelFolder).isNotNull
    assertThat(testregelFolder?.name).isEqualTo("App")
  }

  @Test
  fun readTestregel() {
    val testregel = testregelImportService.getTestregel("1.1.1", TestregelType.Nett,"1.1.1a.json")
    assertThat(testregel).isNotNull
    assertThat(testregel?.name).isEqualTo("1.1.1a.json")
  }

  @Test
  fun getTestregelList() {
    val testreglar = testregelImportService.getTestregelList()
    assertThat(testreglar).isNotEmpty
    println(testreglar)
  }

  @Test
  fun getTestregelContent() {
    val testregel = testregelImportService.getTestregel("1.1.1", TestregelType.Nett,"1.1.1a.json")
    val content = testregel?.let { testregelImportService.getTestregelDataAsString(it) }
    assertThat(content).isNotNull
    println(content)
  }

  @Test
  fun testBase64Decoding() {
    val data = "ewoJIm5hbW4iOiAiMS4xLjFhIEJpbGRlIGhhciB0ZWtzdGFsdGVybmF0aXYi\\nLAoJImlkIjogIjEuMS4xYSIsCgkidGVzdGxhYklkIjogMTUzLAoJInZlcnNq\\nb24iOiAiMS4wIiwKCSJ0eXBlIjogIk5ldHQiLAoJInNwcmFhayI6ICJubiIs\\nCgkia3JhdlRpbFNhbXN2YXIiOiAiPHA+Rm9yIGJpbGRlIGkgSFRNTCBlciBl\\naW4gYXYgZsO4bGdqYW5kZSBlciBvcHBmeWx0OjwvcD48dWw+PGxpPkJpbGRl\\nIHNvbSBlciBweW50IGVyIGtvZGEgcMOlIGVuIHNsaWsgbcOldGUgYXQgZGVp\\nIGlra2plIGVyIHRpbCBzdMO4eS48L2xpPjxsaT5CaWxkZSBzb20gZXIgZWkg\\nc2Fuc2VvcHBsZXZpbmcgZWxsZXIgZWluIHRlc3QgaGFyIGVpdCBrb3J0IHRl\\na3N0YWx0ZXJuYXRpdiBzb20gZ2lyIGVpbiBiZXNrcml2YW5kZSBpZGVudGlm\\naWthc2pvbi48L2xpPjxsaT5CaWxkZSBzb20gZXIgbWVpbmluZ3NiZXJhbmRl\\nIGhhciBlaXQga29ydCB0ZWtzdGFsdGVybmF0aXYgc29tIGdqZW5naXIgc2Ft\\nZSBpbmZvcm1hc2pvbiBzb20gYmlsZXRldC48L2xpPjxsaT5CaWxkZSBzb20g\\nZXIga29tcGxla3NlIGhhciBiw6VkZSBlaXQga29ydCB0ZWtzdGFsdGVybmF0\\naXYgb2cgZWl0IHV0ZnlsbGFuZGUgdGVrc3RhbHRlcm5hdGl2LjwvbGk+PC91\\nbD4iLAoJInNpZGUiOiAiMi4xIiwKCSJlbGVtZW50IjogIjMuMSIsCgkic3Rl\\nZyI6IFsKCQl7CgkJCSJzdGVnbnIiOiAiMi4xIiwKCQkJInNwbSI6ICJLdmEg\\nc2lkZSB0ZXN0YXIgZHUgcMOlPyIsCgkJCSJodCI6ICI8cD5PcHBnaSBVUkwg\\nZWxsZXIgc2lkZS1JRC48L3A+IiwKCQkJInR5cGUiOiAidGVrc3QiLAoJCQki\\nbGFiZWwiOiAiVVJML1NpZGU6IiwKCQkJImRhdGFsaXN0IjogIlNpZGV1dHZh\\nbGciLAoJCQkib2JsaWciOiB0cnVlLAoJCQkicnV0aW5nIjogewoJCQkJImFs\\nbGUiOiB7CgkJCQkJInR5cGUiOiAiZ2FhVGlsIiwKCQkJCQkic3RlZyI6ICIy\\nLjIiCgkJCQl9CgkJCX0KCQl9LAoJCXsKCQkJInN0ZWduciI6ICIyLjIiLAoJ\\nCQkic3BtIjogIkZpbnN0IGRldCBpa2tqZS1sZW5rYSBiaWxkZSBww6UgbmV0\\ndHNpZGE/IiwKCQkJImh0IjogIjxwPkR1IHNrYWwgdGVzdGUgYWxsZSBiaWxk\\nZSBzb20gaWtramUgZXIgbGVua2EuIEJpbGRlIGthbiBmb3IgZWtzZW1wZWwg\\ndmVyZTwvcD48dWw+PGxpPmlsbHVzdHJhc2pvbmFyPC9saT48bGk+cHludGVi\\naWxkZSBvZyBkZWtvcjwvbGk+PGxpPmdyYWZhciBvZyBkaWFncmFtPC9saT48\\nbGk+aWtvbiBvZyBzeW1ib2w8L2xpPjwvdWw+PHA+QmlsZGUga2FuIHZlcmUg\\na29kYSBpbm5pIGFuZHJlIEhUTUwtZWxlbWVudCwgc29tIGZvciBla3NlbXBl\\nbCA8Q29kZT4mI3gzQztmaWd1cmUmI3gzRTs8L2NvZGU+LjwvcD4iLAoJCQki\\ndHlwZSI6ICJqYU5laSIsCgkJCSJydXRpbmciOiB7CgkJCQkiamEiOiB7CgkJ\\nCQkJInR5cGUiOiAiZ2FhVGlsIiwKCQkJCQkic3RlZyI6ICIzLjEiCgkJCQl9\\nLAoJCQkJIm5laSI6IHsKCQkJCQkidHlwZSI6ICJpa2tqZUZvcmVrb21zdCIs\\nCgkJCQkJInV0ZmFsbCI6ICJUZXN0c2lkZSBoYXIgaW5nZW4gaWtramUtbGVu\\na2EgYmlsZGUuIgoJCQkJfQoJCQl9CgkJfSwKCQl7CgkJCSJzdGVnbnIiOiAi\\nMy4xIiwKCQkJInNwbSI6ICJCZXNrcml2IGJpbGRlIiwKCQkJImh0IjogIkR1\\nIGthbiBmb3IgZWtzZW1wZWwgYmVza3JpdmUgbW90aXYsIHBsYXNzZXJpbmcg\\ncMOlIHNpZGEsIGVsZW1lbnQgc29tIGVyIGkgbsOmcmxlaWtlbi4iLAoJCQki\\ndHlwZSI6ICJ0ZWtzdCIsCgkJCSJsYWJlbCI6ICJCaWxkZToiLAoJCQkibXVs\\ndGlsaW5qZSI6IHRydWUsCgkJCSJvYmxpZyI6IHRydWUsCgkJCSJydXRpbmci\\nOiB7CgkJCQkiYWxsZSI6IHsKCQkJCQkidHlwZSI6ICJnYWFUaWwiLAoJCQkJ\\nCSJzdGVnIjogIjMuMiIKCQkJCX0KCQkJfQoJCX0sCgkJewoJCQkic3RlZ25y\\nIjogIjMuMiIsCgkJCSJzcG0iOiAiRXIgYmlsZGUgdGlsIHB5bnQvZGVrb3Ig\\nZWxsZXIgYnJ1a3Qgc29tIGJha2dydW5uIGVsbGVyIHRpbCBmb3JtYXRlcmlu\\nZz8iLAoJCQkiaHQiOiAiRWtzZW1wZWwgcMOlIHNsaWtlIGJpbGRlIGVyIHVz\\neW5saWdlIGJpbGRlLCBnamVubm9tc2lrdGlnZSBiaWxkZSwgcHludGViaWxk\\nZSBvZyBib3JkLiIsCgkJCSJ0eXBlIjogImphTmVpIiwKCQkJInJ1dGluZyI6\\nIHsKCQkJCSJqYSI6IHsKCQkJCQkidHlwZSI6ICJnYWFUaWwiLAoJCQkJCSJz\\ndGVnIjogIjMuMyIKCQkJCX0sCgkJCQkibmVpIjogewoJCQkJCSJ0eXBlIjog\\nImdhYVRpbCIsCgkJCQkJInN0ZWciOiAiMy4xMCIKCQkJCX0KCQkJfQoJCX0s\\nCgkJewoJCQkic3RlZ25yIjogIjMuMyIsCgkJCSJzcG0iOiAiRXIgYmlsZGUg\\na29kYSBtZWQgZWl0IGVsbGVyIGZsZWlyZSBhdiBhdHRyaWJ1dHRhIGFyaWEt\\nbGFiZWwgZWxsZXIgYXJpYS1sYWJlbGxlZGJ5PyIsCgkJCSJodCI6ICJEdSBr\\nYW4gbnl0dGUga29kZXZlcmt0w7h5ZXQgaSBuZXR0bGVzYXJlbiB0aWwgw6Ug\\nc2pla2tlIGRldHRlLiBBdHRyaWJ1dHRhIG92ZXJzdHlyZXIgZXZlbnR1ZWx0\\nIGFsdC1hdHRyaWJ1dHQgZWxsZXIgdGl0bGUtYXR0cmlidXR0LCBzasO4bHYg\\nb20gYXR0cmlidXR0YSBlciB0b21tZS4iLAoJCQkidHlwZSI6ICJqYU5laSIs\\nCgkJCSJraWxkZSI6IFsKCQkJCSJBUklBNiIsCgkJCQkiQVJJQTEwIgoJCQld\\nLAoJCQkicnV0aW5nIjogewoJCQkJImphIjogewoJCQkJCSJ0eXBlIjogImF2\\nc2x1dHQiLAoJCQkJCSJmYXNpdCI6ICJOZWkiLAoJCQkJCSJ1dGZhbGwiOiAi\\nQmlsZGUgc29tIGVyIHB5bnQvZGVrb3IvYmFrZ3J1bm4vZm9ybWF0ZXJpbmcs\\nIGVyIGtvZGEgbWVkIGF0dHJpYnV0dGEgYXJpYS1sYWJlbCBlbGxlciBhcmlh\\nLWxhYmVsbGVkYnkuIgoJCQkJfSwKCQkJCSJuZWkiOiB7CgkJCQkJInR5cGUi\\nOiAiZ2FhVGlsIiwKCQkJCQkic3RlZyI6ICIzLjQiCgkJCQl9CgkJCX0KCQl9\\nLAoJCXsKCQkJInN0ZWduciI6ICIzLjQiLAoJCQkic3BtIjogIkVyIGJpbGRl\\nIGtvZGEgbWVkIGF0dHJpYnV0dGV0IHJvbGU9XCJwcmVzZW50YXRpb25cIj8i\\nLAoJCQkiaHQiOiAiPHA+RHUga2FuIG55dHRlIGtvZGV2ZXJrdMO4eWV0IGkg\\nbmV0dGxlc2FyZW4gdGlsIMOlIHNqZWtrZSBkZXR0ZS48L3A+IiwKCQkJInR5\\ncGUiOiAiamFOZWkiLAoJCQkia2lsZGUiOiBbCgkJCQkiRjM4IgoJCQldLAoJ\\nCQkicnV0aW5nIjogewoJCQkJImphIjogewoJCQkJCSJ0eXBlIjogImF2c2x1\\ndHQiLAoJCQkJCSJmYXNpdCI6ICJKYSIsCgkJCQkJInV0ZmFsbCI6ICJCaWxk\\nZSBzb20gZXIgcHludC9kZWtvci9iYWtncnVubi9mb3JtYXRlcmluZywgaGFy\\nIHRvbXQgdGVrc3RhbHRlcm5hdGl2LiIKCQkJCX0sCgkJCQkibmVpIjogewoJ\\nCQkJCSJ0eXBlIjogImdhYVRpbCIsCgkJCQkJInN0ZWciOiAiMy41IgoJCQkJ\\nfQoJCQl9CgkJfSwKCQl7CgkJCSJzdGVnbnIiOiAiMy41IiwKCQkJInNwbSI6\\nICJFciBiaWxkZSBrb2RhIHNvbSAmI3gzQztpbWcmI3gzRTs/IiwKCQkJImh0\\nIjogIkR1IGthbiBueXR0ZSBrb2RldmVya3TDuHlldCBpIG5ldHRsZXNhcmVu\\nIHRpbCDDpSBzamVra2UgZGV0dGUuIiwKCQkJInR5cGUiOiAiamFOZWkiLAoJ\\nCQkia2lsZGUiOiBbCgkJCQkiSDM3IgoJCQldLAoJCQkicnV0aW5nIjogewoJ\\nCQkJImphIjogewoJCQkJCSJ0eXBlIjogImdhYVRpbCIsCgkJCQkJInN0ZWci\\nOiAiMy42IgoJCQkJfSwKCQkJCSJuZWkiOiB7CgkJCQkJInR5cGUiOiAiZ2Fh\\nVGlsIiwKCQkJCQkic3RlZyI6ICIzLjgiCgkJCQl9CgkJCX0KCQl9LAoJCXsK\\nCQkJInN0ZWduciI6ICIzLjYiLAoJCQkic3BtIjogIkhhciBiaWxkZSBlaXQg\\nYWx0LWF0dHJpYnV0dD8iLAoJCQkiaHQiOiAiRHUga2FuIG55dHRlIGtvZGV2\\nZXJrdMO4eWV0IGkgbmV0dGxlc2FyZW4gdGlsIMOlIHNqZWtrZSBkZXR0ZS4i\\nLAoJCQkidHlwZSI6ICJqYU5laSIsCgkJCSJydXRpbmciOiB7CgkJCQkiamEi\\nOiB7CgkJCQkJInR5cGUiOiAiZ2FhVGlsIiwKCQkJCQkic3RlZyI6ICIzLjci\\nCgkJCQl9LAoJCQkJIm5laSI6IHsKCQkJCQkidHlwZSI6ICJhdnNsdXR0IiwK\\nCQkJCQkiZmFzaXQiOiAiTmVpIiwKCQkJCQkidXRmYWxsIjogIkJpbGRlIHNv\\nbSBlciBweW50L2Rla29yL2Jha2dydW5uL2Zvcm1hdGVyaW5nLCBtYW5nbGFy\\nIGFsdC1hdHRyaWJ1dHQuIgoJCQkJfQoJCQl9CgkJfSwKCQl7CgkJCSJzdGVn\\nbnIiOiAiMy43IiwKCQkJInNwbSI6ICJFciBhbHQtYXR0cmlidXR0ZXQgdG9t\\ndD8iLAoJCQkiaHQiOiAiU2rDpSBpIGtvZGVuIG9tIGFsdC1hdHRyaWJ1dHRl\\ndCBoYXIgaW5uaGFsZC4gRWl0IHRvbXQgYWx0LWF0dHJpYnV0dCBlciBrb2Rh\\nIHDDpSBmb3JtZW4gPGNvZGU+YWx0PVwiXCI8L2NvZGU+IGVsbGVyIGJlcnJl\\nIDxjb2RlPmFsdDwvY29kZT4uIiwKCQkJInR5cGUiOiAiamFOZWkiLAoJCQki\\na2lsZGUiOiBbCgkJCQkiSDY3IiwKCQkJCSJGMzgiLAoJCQkJIkYzOSIKCQkJ\\nXSwKCQkJInJ1dGluZyI6IHsKCQkJCSJqYSI6IHsKCQkJCQkidHlwZSI6ICJn\\nYWFUaWwiLAoJCQkJCSJzdGVnIjogIjMuOCIKCQkJCX0sCgkJCQkibmVpIjog\\newoJCQkJCSJ0eXBlIjogImF2c2x1dHQiLAoJCQkJCSJmYXNpdCI6ICJOZWki\\nLAoJCQkJCSJ1dGZhbGwiOiAiQmlsZGUgc29tIGVyIHB5bnQvZGVrb3IvYmFr\\nZ3J1bm4vZm9ybWF0ZXJpbmcsIGhhciBpa2tqZSB0b210IGFsdC1hdHRyaWJ1\\ndHQuIgoJCQkJfQoJCQl9CgkJfSwKCQl7CgkJCSJzdGVnbnIiOiAiMy44IiwK\\nCQkJInNwbSI6ICJIYXIgYmlsZGUgZWl0IHRpdGxlLWF0dHJpYnV0dD8iLAoJ\\nCQkiaHQiOiAiRHUga2FuIG55dHRlIGtvZGV2ZXJrdMO4eWV0IGkgbmV0dGxl\\nc2FyZW4gdGlsIMOlIHNqZWtrZSBkZXR0ZS4iLAoJCQkidHlwZSI6ICJqYU5l\\naSIsCgkJCSJraWxkZSI6IFsKCQkJCSJGMzgiCgkJCV0sCgkJCSJydXRpbmci\\nOiB7CgkJCQkiamEiOiB7CgkJCQkJInR5cGUiOiAiZ2FhVGlsIiwKCQkJCQki\\nc3RlZyI6ICIzLjkiCgkJCQl9LAoJCQkJIm5laSI6IHsKCQkJCQkidHlwZSI6\\nICJhdnNsdXR0IiwKCQkJCQkiZmFzaXQiOiAiSmEiLAoJCQkJCSJ1dGZhbGwi\\nOiAiQmlsZGUgc29tIGVyIHB5bnQvZGVrb3IvYmFrZ3J1bm4vZm9ybWF0ZXJp\\nbmcsIGhhciB0b210IHRla3N0YWx0ZXJuYXRpdi4iCgkJCQl9CgkJCX0KCQl9\\nLAoJCXsKCQkJInN0ZWduciI6ICIzLjkiLAoJCQkic3BtIjogIkVyIHRpdGxl\\nLWF0dHJpYnV0dGV0IHRvbXQ/IiwKCQkJImh0IjogIlNqw6UgaSBrb2RlbiBv\\nbSB0aXRsZS1hdHRyaWJ1dHRldCBoYXIgaW5uaGFsZC4gRWl0IHRvbXQgdGl0\\nbGUtYXR0cmlidXR0IGVyIGtvZGEgcMOlIGZvcm1lbiA8Y29kZT50aXRsZT1c\\nIlwiPC9jb2RlPiBlbGxlciBiZXJyZSA8Y29kZT50aXRsZTwvY29kZT4uIiwK\\nCQkJInR5cGUiOiAiamFOZWkiLAoJCQkia2lsZGUiOiBbCgkJCQkiRjM4IgoJ\\nCQldLAoJCQkicnV0aW5nIjogewoJCQkJImphIjogewoJCQkJCSJ0eXBlIjog\\nImF2c2x1dHQiLAoJCQkJCSJmYXNpdCI6ICJKYSIsCgkJCQkJInV0ZmFsbCI6\\nICJCaWxkZSBzb20gZXIgcHludC9kZWtvci9iYWtncnVubi9mb3JtYXRlcmlu\\nZywgaGFyIHRvbXQgdGVrc3RhbHRlcm5hdGl2LiIKCQkJCX0sCgkJCQkibmVp\\nIjogewoJCQkJCSJ0eXBlIjogImF2c2x1dHQiLAoJCQkJCSJmYXNpdCI6ICJO\\nZWkiLAoJCQkJCSJ1dGZhbGwiOiAiQmlsZGUgc29tIGVyIHB5bnQvZGVrb3Iv\\nYmFrZ3J1bm4vZm9ybWF0ZXJpbmcsIGhhciBpa2tqZSB0b210IHRpdGxlLWF0\\ndHJpYnV0dC4iCgkJCQl9CgkJCX0KCQl9LAoJCXsKCQkJInN0ZWduciI6ICIz\\nLjEwIiwKCQkJInNwbSI6ICJIYXIgYmlsZGUgYXR0cmlidXR0ZXQgXCJhcmlh\\nLWxhYmVsXCI/IiwKCQkJImh0IjogIjxwPkR1IGthbiBueXR0ZSBrb2RldmVy\\na3TDuHlldCBpIG5ldHRsZXNhcmVuIHRpbCDDpSBzamVra2UgZGV0dGUuIEF0\\ndHJpYnV0dGV0IG92ZXJzdHlyZXIgZXZlbnR1ZWx0IGFsdC1hdHRyaWJ1dHQg\\nZWxsZXIgdGl0bGUtYXR0cmlidXR0LiBNRVJLOiBEdSBza2FsIGlra2plIHZ1\\ncmRlcmUga3ZhbGl0ZXRlbiBww6UgdGVrc3Rlbi4gU2rDpSBpIGtvZGVuIG9n\\nIGZpbm4gZGV0IGFrdHVlbGxlIDxjb2RlPiYjeDNDO2ltZyYjeDNFOzwvY29k\\nZT4tZWxlbWVudGV0LjwvcD48cD4gRWtzZW1wZWw6IDxjb2RlPiYjeDNDO2lt\\nZyBhcmlhLWxhYmVsPVwiQWx0ZXJuYXRpdiB0ZWtzdFwiJiN4M0U7PC9jb2Rl\\nPjwvcD4iLAoJCQkidHlwZSI6ICJqYU5laSIsCgkJCSJraWxkZSI6IFsKCQkJ\\nCSJBUklBNiIsCgkJCQkiRjY1IgoJCQldLAoJCQkicnV0aW5nIjogewoJCQkJ\\nImphIjogewoJCQkJCSJ0eXBlIjogImdhYVRpbCIsCgkJCQkJInN0ZWciOiAi\\nMy4xOSIKCQkJCX0sCgkJCQkibmVpIjogewoJCQkJCSJ0eXBlIjogImdhYVRp\\nbCIsCgkJCQkJInN0ZWciOiAiMy4xMSIKCQkJCX0KCQkJfQoJCX0sCgkJewoJ\\nCQkic3RlZ25yIjogIjMuMTEiLAoJCQkic3BtIjogIkhhciBiaWxkZSBhdHRy\\naWJ1dHRldCBcImFyaWEtbGFiZWxsZWRieVwiIGVsbGVyIFwiYXJpYS1kZXNj\\ncmliZWRieVwiPyIsCgkJCSJodCI6ICJNRVJLOiBEdSBza2FsIGlra2plIHZ1\\ncmRlcmUga3ZhbGl0ZXRlbiBww6UgdGVrc3Rlbi4gTWVyayBhdCBlaW4gYXJp\\nYS1sYWJlbGxlZGJ5IGthbiBpbm5oYWxkZSBmbGVpcmUgaWQtYXIgaSBzYW1l\\nIGF0dHJpYnV0dC4gSWQtYW5lIGVyIHNraWx0IG1lZCBtZWxsb21yb20uICg8\\nY29kZT5BcmlhLWxhYmVsbGVkYnk9XCJpZDEgaWQyXCI8L2NvZGU+KS4iLAoJ\\nCQkidHlwZSI6ICJqYU5laSIsCgkJCSJraWxkZSI6IFsKCQkJCSJBUklBMTAi\\nLAoJCQkJIkFSSUExNSIsCgkJCQkiRjY1IgoJCQldLAoJCQkicnV0aW5nIjog\\newoJCQkJImphIjogewoJCQkJCSJ0eXBlIjogImdhYVRpbCIsCgkJCQkJInN0\\nZWciOiAiMy4xMiIKCQkJCX0sCgkJCQkibmVpIjogewoJCQkJCSJ0eXBlIjog\\nImdhYVRpbCIsCgkJCQkJInN0ZWciOiAiMy4xMyIKCQkJCX0KCQkJfQoJCX0s\\nCgkJewoJCQkic3RlZ25yIjogIjMuMTIiLAoJCQkic3BtIjogIkVyIGFyaWEt\\nbGFiZWxsZWRieS9hcmlhLWRlc2NyaWJlZGJ5IGF0dHJpYnV0dGV0IGtvcGxh\\nIHRpbCBhbm5hbiB0ZWtzdCBww6Ugc2lkYT8iLAoJCQkiaHQiOiAiPHA+R2pl\\nciBlaXQgc8O4ayBpIGtvZGVuIHDDpSBpZCBpIGFyaWEtbGFiZWxsZWRieS4g\\nRGVyc29tIGRldCBmaW5zdCBmbGVpcmUgaWQtYXIgc2thbCBkdSB1bmRlcnPD\\nuGtlIGFsbGUuIElkLWFuZSB2aWwgZMOlIHZlcmUgc2tpbHQgbWVkIG1lbGxv\\nbXJvbS4gKEFyaWEtbGFiZWxsZWRieT1cImlkMSBpZDJcIikuPC9wPiIsCgkJ\\nCSJ0eXBlIjogImphTmVpIiwKCQkJImtpbGRlIjogWwoJCQkJIkFSSUExMCIs\\nCgkJCQkiQVJJQTE1IiwKCQkJCSJGNjUiCgkJCV0sCgkJCSJydXRpbmciOiB7\\nCgkJCQkiamEiOiB7CgkJCQkJInR5cGUiOiAiZ2FhVGlsIiwKCQkJCQkic3Rl\\nZyI6ICIzLjE5IgoJCQkJfSwKCQkJCSJuZWkiOiB7CgkJCQkJInR5cGUiOiAi\\nZ2FhVGlsIiwKCQkJCQkic3RlZyI6ICIzLjEzIgoJCQkJfQoJCQl9CgkJfSwK\\nCQl7CgkJCSJzdGVnbnIiOiAiMy4xMyIsCgkJCSJzcG0iOiAiRXIgYmlsZGUg\\na29kYSBtZWQgYXR0cmlidXR0ZXQgcm9sZT1cInByZXNlbnRhdGlvblwiPyIs\\nCgkJCSJodCI6ICJEdSBrYW4gbnl0dGUga29kZXZlcmt0w7h5ZXQgaSBuZXR0\\nbGVzYXJlbiB0aWwgw6Ugc2pla2tlIGRldHRlLiIsCgkJCSJ0eXBlIjogImph\\nTmVpIiwKCQkJImtpbGRlIjogWwoJCQkJIkYzOCIKCQkJXSwKCQkJInJ1dGlu\\nZyI6IHsKCQkJCSJqYSI6IHsKCQkJCQkidHlwZSI6ICJhdnNsdXR0IiwKCQkJ\\nCQkiZmFzaXQiOiAiTmVpIiwKCQkJCQkidXRmYWxsIjogIk1laW5pbmdzYmVy\\nYW5kZSBiaWxkZSBlciBrb2RhIG1lZCByb2xlPVwicHJlc2VudGF0aW9uXCIu\\nIgoJCQkJfSwKCQkJCSJuZWkiOiB7CgkJCQkJInR5cGUiOiAiZ2FhVGlsIiwK\\nCQkJCQkic3RlZyI6ICIzLjE0IgoJCQkJfQoJCQl9CgkJfSwKCQl7CgkJCSJz\\ndGVnbnIiOiAiMy4xNCIsCgkJCSJzcG0iOiAiRXIgYmlsZGUga29kYSBzb20g\\nJiN4M0M7aW1nJiN4M0U7PyIsCgkJCSJodCI6ICJEdSBrYW4gbnl0dGUga29k\\nZXZlcmt0w7h5ZXQgaSBuZXR0bGVzYXJlbiB0aWwgw6Ugc2pla2tlIGRldHRl\\nLiIsCgkJCSJ0eXBlIjogImphTmVpIiwKCQkJImtpbGRlIjogWwoJCQkJIkgz\\nNyIKCQkJXSwKCQkJInJ1dGluZyI6IHsKCQkJCSJqYSI6IHsKCQkJCQkidHlw\\nZSI6ICJnYWFUaWwiLAoJCQkJCSJzdGVnIjogIjMuMTUiCgkJCQl9LAoJCQkJ\\nIm5laSI6IHsKCQkJCQkidHlwZSI6ICJnYWFUaWwiLAoJCQkJCSJzdGVnIjog\\nIjMuMTciCgkJCQl9CgkJCX0KCQl9LAoJCXsKCQkJInN0ZWduciI6ICIzLjE1\\nIiwKCQkJInNwbSI6ICJIYXIgYmlsZGUgZWl0IGFsdC1hdHRyaWJ1dHQ/IiwK\\nCQkJImh0IjogIkR1IGthbiBueXR0ZSBrb2RldmVya3TDuHlldCBpIG5ldHRs\\nZXNhcmVuIHRpbCDDpSBzamVra2UgZGV0dGUuIiwKCQkJInR5cGUiOiAiamFO\\nZWkiLAoJCQkia2lsZGUiOiBbCgkJCQkiSDM3IiwKCQkJCSJGNjUiCgkJCV0s\\nCgkJCSJydXRpbmciOiB7CgkJCQkiamEiOiB7CgkJCQkJInR5cGUiOiAiZ2Fh\\nVGlsIiwKCQkJCQkic3RlZyI6ICIzLjE2IgoJCQkJfSwKCQkJCSJuZWkiOiB7\\nCgkJCQkJInR5cGUiOiAiYXZzbHV0dCIsCgkJCQkJImZhc2l0IjogIk5laSIs\\nCgkJCQkJInV0ZmFsbCI6ICJNZWluaW5nc2JlcmFuZGUgYmlsZGUgbWFuZ2xh\\nciBhbHQtYXR0cmlidXR0LiIKCQkJCX0KCQkJfQoJCX0sCgkJewoJCQkic3Rl\\nZ25yIjogIjMuMTYiLAoJCQkic3BtIjogIkVyIGRldCBpbm5oYWxkIGkgYWx0\\nLWF0dHJpYnV0dGV0PyIsCgkJCSJodCI6ICI8cD5TasOlIGkga29kZW4gb20g\\nYWx0LWF0dHJpYnV0dGV0IGhhciBpbm5oYWxkLiBFaXQgdG9tdCBhbHQtYXR0\\ncmlidXR0IGVyIGtvZGEgcMOlIGZvcm1lbiA8Y29kZT5hbHQ9XCJcIjwvY29k\\nZT4gZWxsZXIgYmVycmUgPGNvZGU+YWx0PC9jb2RlPi48L3A+IiwKCQkJInR5\\ncGUiOiAiamFOZWkiLAoJCQkia2lsZGUiOiBbCgkJCQkiSDM3IgoJCQldLAoJ\\nCQkicnV0aW5nIjogewoJCQkJImphIjogewoJCQkJCSJ0eXBlIjogImdhYVRp\\nbCIsCgkJCQkJInN0ZWciOiAiMy4xOSIKCQkJCX0sCgkJCQkibmVpIjogewoJ\\nCQkJCSJ0eXBlIjogImdhYVRpbCIsCgkJCQkJInN0ZWciOiAiMy4xNyIKCQkJ\\nCX0KCQkJfQoJCX0sCgkJewoJCQkic3RlZ25yIjogIjMuMTciLAoJCQkic3Bt\\nIjogIkhhciBiaWxkZSBlaXQgdGl0bGUtYXR0cmlidXR0PyIsCgkJCSJodCI6\\nICJEdSBrYW4gbnl0dGUga29kZXZlcmt0w7h5ZXQgaSBuZXR0bGVzYXJlbiB0\\naWwgw6Ugc2pla2tlIGRldHRlLiIsCgkJCSJ0eXBlIjogImphTmVpIiwKCQkJ\\nImtpbGRlIjogWwoJCQkJIkY2NSIKCQkJXSwKCQkJInJ1dGluZyI6IHsKCQkJ\\nCSJqYSI6IHsKCQkJCQkidHlwZSI6ICJnYWFUaWwiLAoJCQkJCSJzdGVnIjog\\nIjMuMTgiCgkJCQl9LAoJCQkJIm5laSI6IHsKCQkJCQkidHlwZSI6ICJhdnNs\\ndXR0IiwKCQkJCQkiZmFzaXQiOiAiTmVpIiwKCQkJCQkidXRmYWxsIjogIk1l\\naW5pbmdzYmVyYW5kZSBiaWxkZSBtYW5nbGFyIHRla3N0YWx0ZXJuYXRpdi4i\\nCgkJCQl9CgkJCX0KCQl9LAoJCXsKCQkJInN0ZWduciI6ICIzLjE4IiwKCQkJ\\nInNwbSI6ICJFciBkZXQgaW5uaGFsZCBpIHRpdGxlLWF0dHJpYnV0dGV0PyIs\\nCgkJCSJodCI6ICJTasOlIGkga29kZW4gb20gdGl0bGUtYXR0cmlidXR0ZXQg\\naGFyIGlubmhhbGQuIEVpdCB0b210IHRpdGxlLWF0dHJpYnV0dCBlciBrb2Rh\\nIHDDpSBmb3JtZW4gdGl0bGU9XCJcIiBlbGxlciBiZXJyZSB0aXRsZS4iLAoJ\\nCQkidHlwZSI6ICJqYU5laSIsCgkJCSJraWxkZSI6IFsKCQkJCSJGNjUiCgkJ\\nCV0sCgkJCSJydXRpbmciOiB7CgkJCQkiamEiOiB7CgkJCQkJInR5cGUiOiAi\\nZ2FhVGlsIiwKCQkJCQkic3RlZyI6ICIzLjE5IgoJCQkJfSwKCQkJCSJuZWki\\nOiB7CgkJCQkJInR5cGUiOiAiYXZzbHV0dCIsCgkJCQkJImZhc2l0IjogIk5l\\naSIsCgkJCQkJInV0ZmFsbCI6ICJNZWluaW5nc2JlcmFuZGUgYmlsZGUgbWFu\\nZ2xhciB0ZWtzdGFsdGVybmF0aXYuIgoJCQkJfQoJCQl9CgkJfSwKCQl7CgkJ\\nCSJzdGVnbnIiOiAiMy4xOSIsCgkJCSJzcG0iOiAiRXIgYmlsZGUgZWluIHRl\\nc3QgZWxsZXIgZWkgc2Fuc2VvcHBsZXZpbmc/IiwKCQkJImh0IjogIjxwPjxi\\nPlRlc3Q6IDwvYj5CaWxkZSBzb20gZXIgZWluIGRlbCBhdiBlaW4gdGVzdCBl\\nbGxlciDDuHZpbmcgZXIgYmlsZGUgZGVyIGlubmhhbGRldCB2aWwgYmxpIHVn\\neWxkaWcgZGVyc29tIGRldCBibGlyIHByZXNlbnRlcnQgc29tIHRla3N0LiBI\\nZW5zaWt0ZW4gbWVkIHRlc3RlbiB2aWwgZm9ydmlubmUgZGVyc29tIHN2YXJl\\ndCBibGlyIGF2c2zDuHJ0IGF2IHRla3N0YWx0ZXJuYXRpdmV0LjwvcD48cD48\\nYj5TYW5zZW9wcGxldmluZzogPC9iPkJpbGRlIHNvbSBza2FsIGdpIGVpIHNh\\nbnNlb3BwbGV2aW5nIGVyIGJpbGRlIHNvbSBpa2tqZSBiZXJyZSBlciB0aWwg\\ncHludCwgbWVuIHNvbSBpa2tqZSBoYXIgc29tIGhvdnVkZm9ybcOlbCDDpSBm\\nb3JtaWRsZSBpbmZvcm1hc2pvbi4gRWl0IG1hbGVyaSBlciBla3NlbXBlbCBw\\nw6UgZWkgc2Fuc2VvcHBsZXZpbmcuPC9wPiIsCgkJCSJ0eXBlIjogImphTmVp\\nIiwKCQkJImtpbGRlIjogWwoJCQkJIkc5NCIsCgkJCQkiRzEwMCIsCgkJCQki\\nRjMwIgoJCQldLAoJCQkicnV0aW5nIjogewoJCQkJImphIjogewoJCQkJCSJ0\\neXBlIjogImdhYVRpbCIsCgkJCQkJInN0ZWciOiAiMy4yMCIKCQkJCX0sCgkJ\\nCQkibmVpIjogewoJCQkJCSJ0eXBlIjogImdhYVRpbCIsCgkJCQkJInN0ZWci\\nOiAiMy4yMSIKCQkJCX0KCQkJfQoJCX0sCgkJewoJCQkic3RlZ25yIjogIjMu\\nMjAiLAoJCQkic3BtIjogIkdpciBpbm5oYWxkZXQgaSBhdHRyaWJ1dHRldCBl\\naW4gYmVza3JpdmFuZGUgaWRlbnRpZmlrYXNqb24gYXYgYmlsZGU/IiwKCQkJ\\nImh0IjogIjxwPkdqZXIgZWkgc2tqw7hubnNtZXNzaWcgdnVyZGVyaW5nIGF2\\nIG9tIGluZm9ybWFzam9uZW4gaSBhdHRyaWJ1dHRldCAoZHZzLiB0ZWtzdGFs\\ndGVybmF0aXZldCkgaWRlbnRpZmlzZXJlciBpbm5oYWxkZXQgaSBiaWxkZS4g\\nVmlzcyBhcmlhLWxhYmVsbGVkYnkgdmlzYXIgdGlsIGR1cGxpc2VydGUgaWQt\\nYXIsIHNrYWwgZHUgdnVyZGVyZSBpbm5oYWxkZXQgdGlsIGlkLWVuIHNvbSBz\\ndMOlciBmw7hyc3QgaSBrb2Rlbi48L3A+PHA+PHN0cm9uZz5NZXJrOjwvc3Ry\\nb25nPiBGaWxuYW1uLCBrb3IgdGVrc3RhbHRlcm5hdGl2ZXQgaW5uZWhlbGQg\\nZWl0IGZpbGV0dGVybmFtbiBzb20gZm9yIGVrc2VtcGVsIC5qcGcgZWxsZXIg\\nLnBuZywgZXIgYWxsdGlkIGZlaWwuPC9wPiIsCgkJCSJ0eXBlIjogImphTmVp\\nIiwKCQkJImtpbGRlIjogWwoJCQkJIkc5NCIsCgkJCQkiRzEwMCIsCgkJCQki\\nRjMwIgoJCQldLAoJCQkicnV0aW5nIjogewoJCQkJImphIjogewoJCQkJCSJ0\\neXBlIjogImF2c2x1dHQiLAoJCQkJCSJmYXNpdCI6ICJKYSIsCgkJCQkJInV0\\nZmFsbCI6ICJCaWxkZSBzb20gZXIgZWkgc2Fuc2VvcHBsZXZpbmcgZWxsZXIg\\nZWluIHRlc3QsIGhhciBiZXNrcml2YW5kZSB0ZWtzdGFsdGVybmF0aXYuIgoJ\\nCQkJfSwKCQkJCSJuZWkiOiB7CgkJCQkJInR5cGUiOiAiYXZzbHV0dCIsCgkJ\\nCQkJImZhc2l0IjogIk5laSIsCgkJCQkJInV0ZmFsbCI6ICJCaWxkZSBzb20g\\nZXIgZWkgc2Fuc2VvcHBsZXZpbmcgZWxsZXIgZWluIHRlc3QsIGhhciBpa2tq\\nZSBiZXNrcml2YW5kZSB0ZWtzdGFsdGVybmF0aXYuIgoJCQkJfQoJCQl9CgkJ\\nfSwKCQl7CgkJCSJzdGVnbnIiOiAiMy4yMSIsCgkJCSJzcG0iOiAiR2lyIGlu\\nbmhhbGRldCBpIGF0dHJpYnV0dGV0IGRlbiBzYW1lIGluZm9ybWFzam9uZW4g\\nc29tIGVyIGZvcm1pZGxhIGF2IGJpbGRlPyIsCgkJCSJodCI6ICI8cD5HamVy\\nIGVpIHNrasO4bm5zbWVzc2lnIHZ1cmRlcmluZyBhdiBvbSBpbmZvcm1hc2pv\\nbmVuIGkgYXR0cmlidXR0ZXQgKGR2cy4gdGVrc3RhbHRlcm5hdGl2ZXQpIGdp\\nciB0aWxzdHJla2tlbGVnIGluZm9ybWFzam9uLiBFaXQgZ29kdCB0ZWtzdGFs\\ndGVybmF0aXYgZ2plciBkZXQgbW9nbGVnIMOlIGVyc3RhdHRlIGJpbGRlIG1l\\nZCB0ZWtzdGFsdGVybmF0aXZldCB1dGVuIMOlIG1pc3RlIGZ1bmtzam9uYWxp\\ndGV0IGVsbGVyIGluZm9ybWFzam9uLiBWaXNzIGFyaWEtbGFiZWxsZWRieSB2\\naXNhciB0aWwgZHVwbGlzZXJ0ZSBpZC1hciwgc2thbCBkdSB2dXJkZXJlIGlu\\nbmhhbGRldCB0aWwgaWQtZW4gc29tIHN0w6VyIGbDuHJzdCBpIGtvZGVuLiBN\\nZXJrOiBGaWxuYW1uLCBrb3IgdGVrc3RhbHRlcm5hdGl2ZXQgaW5uZWhlbGQg\\nZWl0IGZpbGV0dGVybmFtbiBzb20gZm9yIGVrc2VtcGVsIC5qcGcgZWxsZXIg\\nLnBuZywgZXIgYWxsdGlkIGZlaWwuPC9wPiIsCgkJCSJ0eXBlIjogImphTmVp\\nIiwKCQkJImtpbGRlIjogWwoJCQkJIkYzMCIsCgkJCQkiRzk0IgoJCQldLAoJ\\nCQkicnV0aW5nIjogewoJCQkJImphIjogewoJCQkJCSJ0eXBlIjogImdhYVRp\\nbCIsCgkJCQkJInN0ZWciOiAiMy4yMiIKCQkJCX0sCgkJCQkibmVpIjogewoJ\\nCQkJCSJ0eXBlIjogImdhYVRpbCIsCgkJCQkJInN0ZWciOiAiMy4yMiIKCQkJ\\nCX0KCQkJfQoJCX0sCgkJewoJCQkic3RlZ25yIjogIjMuMjIiLAoJCQkic3Bt\\nIjogIkVyIGJpbGRlIGtvbXBsZWtzdD8iLAoJCQkiaHQiOiAiRWtzZW1wZWwg\\ncMOlIGtvbXBsZWtzZSBiaWxkZSBlciBncmFmYXIsIGRpYWdyYW0gZWxsZXIg\\nYW5kcmUgYmlsZGUgc29tIGlubmVoZWxkIG15a2plIGluZm9ybWFzam9uLiIs\\nCgkJCSJ0eXBlIjogImphTmVpIiwKCQkJImtpbGRlIjogWwoJCQkJIkc5NSIK\\nCQkJXSwKCQkJInJ1dGluZyI6IHsKCQkJCSJqYSI6IHsKCQkJCQkidHlwZSI6\\nICJnYWFUaWwiLAoJCQkJCSJzdGVnIjogIjMuMjMiCgkJCQl9LAoJCQkJIm5l\\naSI6IHsKCQkJCQkidHlwZSI6ICJyZWdsZXIiLAoJCQkJCSJyZWdsZXIiOiB7\\nCgkJCQkJCSIxIjogewoJCQkJCQkJInNqZWtrIjogIjMuMjEiLAoJCQkJCQkJ\\nInR5cGUiOiAibGlrIiwKCQkJCQkJCSJ2ZXJkaSI6ICJOZWkiLAoJCQkJCQkJ\\nImhhbmRsaW5nIjogewoJCQkJCQkJCSJ0eXBlIjogImF2c2x1dHQiLAoJCQkJ\\nCQkJCSJmYXNpdCI6ICJOZWkiLAoJCQkJCQkJCSJ1dGZhbGwiOiAiTWVpbmlu\\nZ3NiZXJhbmRlIGJpbGRlIGhhciB0ZWtzdGFsdGVybmF0aXYgc29tIGlra2pl\\nIGVyIGJlc2tyaXZhbmRlLiIKCQkJCQkJCX0KCQkJCQkJfSwKCQkJCQkJIjIi\\nOiB7CgkJCQkJCQkic2pla2siOiAiMy4yMSIsCgkJCQkJCQkidHlwZSI6ICJs\\naWsiLAoJCQkJCQkJInZlcmRpIjogIkphIiwKCQkJCQkJCSJoYW5kbGluZyI6\\nIHsKCQkJCQkJCQkidHlwZSI6ICJhdnNsdXR0IiwKCQkJCQkJCQkiZmFzaXQi\\nOiAiSmEiLAoJCQkJCQkJCSJ1dGZhbGwiOiAiTWVpbmluZ3NiZXJhbmRlIGJp\\nbGRlIGhhciBiZXNrcml2YW5kZSB0ZWtzdGFsdGVybmF0aXYuIgoJCQkJCQkJ\\nfQoJCQkJCQl9CgkJCQkJfQoJCQkJfQoJCQl9CgkJfSwKCQl7CgkJCSJzdGVn\\nbnIiOiAiMy4yMyIsCgkJCSJzcG0iOiAiR2lyIGlubmhhbGRldCBpIGF0dHJp\\nYnV0dGV0IGVpbiBiZXNrcml2YW5kZSBpZGVudGlmaWthc2pvbiBhdiBiaWxk\\nZT8iLAoJCQkiaHQiOiAiTWVyayBhdCB2aSBoZXIga3VuIGtyZXZlciBlaW4g\\naWRlbnRpZmlrYXNqb24gaSBkZXQga29ydGUgdGVrc3RhbHRlcm5hdGl2ZXQg\\ndGlsIGtvbXBsZWtzZSBiaWxkZS4gR2plciBlaSBza2rDuG5uc21lc3NpZyB2\\ndXJkZXJpbmcgYXYgb20gaW5mb3JtYXNqb25lbiBpIGF0dHJpYnV0dGV0IChk\\ndnMuIHRla3N0YWx0ZXJuYXRpdmV0KSBpZGVudGlmaXNlcmVyIGlubmhhbGRl\\ndCBpIGJpbGRlLiBWaXNzIGFyaWEtbGFiZWxsZWRieSB2aXNhciB0aWwgZHVw\\nbGlzZXJ0ZSBpZC1hciwgc2thbCBkdSB2dXJkZXJlIGlubmhhbGRldCB0aWwg\\naWQtZW4gc29tIHN0w6VyIGbDuHJzdCBpIGtvZGVuLiBGaWxuYW1uLCBrb3Ig\\ndGVrc3RhbHRlcm5hdGl2ZXQgaW5uZWhlbGQgZWl0IGZpbGV0dGVybmFtbiBz\\nb20gZm9yIGVrc2VtcGVsIC5qcGcgZWxsZXIgLnBuZywgZXIgYWxsdGlkIGZl\\naWwuIiwKCQkJInR5cGUiOiAiamFOZWkiLAoJCQkia2lsZGUiOiBbCgkJCQki\\nRzk1IgoJCQldLAoJCQkicnV0aW5nIjogewoJCQkJImphIjogewoJCQkJCSJ0\\neXBlIjogImdhYVRpbCIsCgkJCQkJInN0ZWciOiAiMy4yNCIKCQkJCX0sCgkJ\\nCQkibmVpIjogewoJCQkJCSJ0eXBlIjogImF2c2x1dHQiLAoJCQkJCSJmYXNp\\ndCI6ICJOZWkiLAoJCQkJCSJ1dGZhbGwiOiAiS29tcGxla3N0IGJpbGRlIGhh\\nciBrb3J0IHRla3N0YWx0ZXJuYXRpdiBzb20gaWtramUgZXIgYmVza3JpdmFu\\nZGUuIgoJCQkJfQoJCQl9CgkJfSwKCQl7CgkJCSJzdGVnbnIiOiAiMy4yNCIs\\nCgkJCSJzcG0iOiAiTGlnZyBkZXQgZWluIHV0ZnlsbGFuZGUgdGVrc3Qgb20g\\nYmlsZGUgaSBkaXJla3RlIHRpbGtueXRuaW5nIHRpbCBiaWxkZT8iLAoJCQki\\naHQiOiAiVGVrc3RhbHRlcm5hdGl2ZXQgZXIgc3lubGlnIGZvciBhbGxlIGJy\\ndWtlcmUgb2cgc2thbCBiZXNrcml2ZSBpbm5oYWxkZXQgaSBiaWxkZS4gRm9y\\nIMOlIHZlcmUgaSBkaXJla3RlIHRpbGtueXRuaW5nLCBza2FsIHRla3N0ZW4g\\nbGlnZ2UgYW50ZW4gcmV0dCBmw7hyIGVsbGVyIHJldHQgZXR0ZXIgYmlsZGUg\\naSBrb2Rlbi4iLAoJCQkidHlwZSI6ICJqYU5laSIsCgkJCSJraWxkZSI6IFsK\\nCQkJCSJHNzQiCgkJCV0sCgkJCSJydXRpbmciOiB7CgkJCQkiamEiOiB7CgkJ\\nCQkJInR5cGUiOiAiZ2FhVGlsIiwKCQkJCQkic3RlZyI6ICIzLjI5IgoJCQkJ\\nfSwKCQkJCSJuZWkiOiB7CgkJCQkJInR5cGUiOiAiZ2FhVGlsIiwKCQkJCQki\\nc3RlZyI6ICIzLjI1IgoJCQkJfQoJCQl9CgkJfSwKCQl7CgkJCSJzdGVnbnIi\\nOiAiMy4yNSIsCgkJCSJzcG0iOiAiVmlzZXIgYXR0cmlidXR0ZXQgcMOlIGJp\\nbGRlIHRpbCBlaW4gdXRmeWxsYW5kZSB0ZWtzdCBzb20gbGlnZyBww6Ugc2Ft\\nZSBzaWRlIHNvbSBiaWxkZT8iLAoJCQkiaHQiOiAiRGV0IGtvcnRlIHRla3N0\\nYWx0ZXJuYXRpdmV0IHDDpSBiaWxkZSBza2FsIHZpc2UgdGlsIGtvciBicnVr\\nYXJlbiBrYW4gZmlubmUgZXQgdXRmeWxsYW5kZSB0ZWtzdGFsdGVybmF0aXYg\\nbWVkIG1laXIgaW5mb3JtYXNqb24uIiwKCQkJInR5cGUiOiAiamFOZWkiLAoJ\\nCQkia2lsZGUiOiBbCgkJCQkiRzc0IgoJCQldLAoJCQkicnV0aW5nIjogewoJ\\nCQkJImphIjogewoJCQkJCSJ0eXBlIjogImdhYVRpbCIsCgkJCQkJInN0ZWci\\nOiAiMy4yOSIKCQkJCX0sCgkJCQkibmVpIjogewoJCQkJCSJ0eXBlIjogImdh\\nYVRpbCIsCgkJCQkJInN0ZWciOiAiMy4yNiIKCQkJCX0KCQkJfQoJCX0sCgkJ\\newoJCQkic3RlZ25yIjogIjMuMjYiLAoJCQkic3BtIjogIkxpZ2cgZGV0IGVp\\nIGxlbmtlLCBpIGRpcmVrdGUgdGlsa255dG5pbmcgdGlsIGJpbGRlLCBzb20g\\ndGVrIGRlZyB0aWwgZWluIHV0ZnlsbGFuZGUgdGVrc3Qgb20gYmlsZGU/IiwK\\nCQkJImh0IjogIkZvciDDpSB2ZXJlIGkgZGlyZWt0ZSB0aWxrbnl0bmluZywg\\nc2thbCB0ZWtzdGVuIGxpZ2dlIGFudGVuIHJldHQgZsO4ciBlbGxlciByZXR0\\nIGV0dGVyIGJpbGRlIGkga29kZW4uIExlbmthIGthbiBwZWlrZSB0aWwgdGVr\\nc3Qgc29tIGxpZ2cgcMOlIGVpIGFubmEgbmV0dHNpZGUsIGVsbGVyIHDDpSBz\\nYW1lIHNpZGUgc29tIGJpbGRlLiIsCgkJCSJ0eXBlIjogImphTmVpIiwKCQkJ\\nImtpbGRlIjogWwoJCQkJIkc3MyIKCQkJXSwKCQkJInJ1dGluZyI6IHsKCQkJ\\nCSJqYSI6IHsKCQkJCQkidHlwZSI6ICJnYWFUaWwiLAoJCQkJCSJzdGVnIjog\\nIjMuMjkiCgkJCQl9LAoJCQkJIm5laSI6IHsKCQkJCQkidHlwZSI6ICJnYWFU\\naWwiLAoJCQkJCSJzdGVnIjogIjMuMjciCgkJCQl9CgkJCX0KCQl9LAoJCXsK\\nCQkJInN0ZWduciI6ICIzLjI3IiwKCQkJInNwbSI6ICJFciBiaWxkZSBrb2Rh\\nIG1lZCBhdHRyaWJ1dHRldCBsb25nZGVzYz8iLAoJCQkiaHQiOiAiRHUga2Fu\\nIG55dHRlIGtvZGV2ZXJrdMO4eWV0IGkgbmV0dGxlc2FyZW4gdGlsIMOlIHNq\\nZWtrZSBkZXR0ZS4iLAoJCQkidHlwZSI6ICJqYU5laSIsCgkJCSJraWxkZSI6\\nIFsKCQkJCSJINDUiCgkJCV0sCgkJCSJydXRpbmciOiB7CgkJCQkiamEiOiB7\\nCgkJCQkJInR5cGUiOiAiZ2FhVGlsIiwKCQkJCQkic3RlZyI6ICIzLjI4IgoJ\\nCQkJfSwKCQkJCSJuZWkiOiB7CgkJCQkJInR5cGUiOiAiYXZzbHV0dCIsCgkJ\\nCQkJImZhc2l0IjogIk5laSIsCgkJCQkJInV0ZmFsbCI6ICJLb21wbGVrc3Qg\\nYmlsZGUgaGFyIGlra2plIGtvcGxpbmcgdGlsIGxhbmd0IHRla3N0YWx0ZXJu\\nYXRpdi4iCgkJCQl9CgkJCX0KCQl9LAoJCXsKCQkJInN0ZWduciI6ICIzLjI4\\nIiwKCQkJInNwbSI6ICJGdW5nZXJlciBsZW5rYSBzb20gbGlnZyBpIGxvbmdk\\nZXNjLWF0dHJpYnV0dGV0PyIsCgkJCSJodCI6ICJLb3BpZXIgVVJMIHNvbSBs\\naWdnIGkgbG9uZ2Rlc2Mgb2cgb3BuZSBpIG5ldHRsZXNhcmVuLiIsCgkJCSJ0\\neXBlIjogImphTmVpIiwKCQkJImtpbGRlIjogWwoJCQkJIkg0NSIKCQkJXSwK\\nCQkJInJ1dGluZyI6IHsKCQkJCSJqYSI6IHsKCQkJCQkidHlwZSI6ICJnYWFU\\naWwiLAoJCQkJCSJzdGVnIjogIjMuMjkiCgkJCQl9LAoJCQkJIm5laSI6IHsK\\nCQkJCQkidHlwZSI6ICJhdnNsdXR0IiwKCQkJCQkiZmFzaXQiOiAiTmVpIiwK\\nCQkJCQkidXRmYWxsIjogIktvbXBsZWtzdCBiaWxkZSBoYXIgaWtramUga29w\\nbGluZyB0aWwgbGFuZ3QgdGVrc3RhbHRlcm5hdGl2LiIKCQkJCX0KCQkJfQoJ\\nCX0sCgkJewoJCQkic3RlZ25yIjogIjMuMjkiLAoJCQkic3BtIjogIkVyIHRl\\na3N0YWx0ZXJuYXRpdmV0IGtvZGEgc29tIHRla3N0PyIsCgkJCSJodCI6ICI8\\ncD5UZWtzdGVuIGthbiB2ZXJlIGzDuHBhbmRlIHRla3N0LCB0YWJlbGwgZWxs\\nZXIgbGlnbmFuZGUuIERldHRlIGthbiB1bmRlcnPDuGthc3QgcMOlIGZsZWly\\nZSBtw6V0YXI6PC9wPjx1bD48bGk+QWx0ZXJuYXRpdiAxOiBTasOlIG9tIGR1\\nIGbDpXIgdGlsIMOlIG1hcmtlcmUgdGVrc3RlbiBtZWQgbXVzIGVsbGVyIHRh\\nc3RhdHVyLiBEZXR0ZSBpbmRpa2VyZXIgYXQgdGVrc3RlbiBlciBrb2RhIHNv\\nbSB0ZWtzdCBvZyBpa2tqZSBlciBlaXQgYmlsZGUgYXYgdGVrc3QuPC9saT48\\nbGk+QWx0ZXJuYXRpdiAyOiBTamVrayBhdCB0ZWtzdGFsdGVybmF0aXZldCBl\\nciBrb2RhIHNvbSB0ZWtzdCwgdmVkIMOlIHNqw6Ugb20gZHUgZmlubiBhdHQg\\ndGVrc3RlbiBpIGtvZGVuLjwvbGk+PC91bD4iLAoJCQkidHlwZSI6ICJqYU5l\\naSIsCgkJCSJydXRpbmciOiB7CgkJCQkiamEiOiB7CgkJCQkJInR5cGUiOiAi\\nZ2FhVGlsIiwKCQkJCQkic3RlZyI6ICIzLjMwIgoJCQkJfSwKCQkJCSJuZWki\\nOiB7CgkJCQkJInR5cGUiOiAiYXZzbHV0dCIsCgkJCQkJImZhc2l0IjogIk5l\\naSIsCgkJCQkJInV0ZmFsbCI6ICJLb21wbGVrc3QgYmlsZGUgaGFyIGxhbmd0\\nIHRla3N0YWx0ZXJuYXRpdiBzb20gaWtramUgZXIga29kYSBzb20gdGVrc3Qu\\nIgoJCQkJfQoJCQl9LAoJCQkia2lsZGUiOiBbXQoJCX0sCgkJewoJCQkic3Rl\\nZ25yIjogIjMuMzAiLAoJCQkic3BtIjogIkdpciBpbm5oYWxkZXQgaSBkZW4g\\ndXRmeWxsYW5kZSB0ZWtzdGVuIGVpIHNraWxkcmluZyBhdiBpbm5oYWxkZXQg\\naSBiaWxkZT8iLAoJCQkiaHQiOiAiPHA+R2plciBlaSBza2rDuG5uc21lc3Np\\nZyB2dXJkZXJpbmcgYXYgb20gaW5mb3JtYXNqb25lbiBpIGRldCB1dGZ5bGxh\\nbmRlIHRla3N0YWx0ZXJuYXRpdmV0IGdpciB0aWxzdHJla2tlbGVnIGluZm9y\\nbWFzam9uLiBFaXQgZ29kdCB0ZWtzdGFsdGVybmF0aXYgZ2plciBkZXQgbW9n\\nbGVnIMOlIGVyc3RhdHRlIGJpbGRlIG1lZCB0ZWtzdGFsdGVybmF0aXZldCB1\\ndGVuIMOlIG1pc3RlIGZ1bmtzam9uYWxpdGV0IGVsbGVyIGluZm9ybWFzam9u\\nLjwvcD4iLAoJCQkidHlwZSI6ICJqYU5laSIsCgkJCSJraWxkZSI6IFsKCQkJ\\nCSJGNjciLAoJCQkJIkc5MiIKCQkJXSwKCQkJInJ1dGluZyI6IHsKCQkJCSJq\\nYSI6IHsKCQkJCQkidHlwZSI6ICJhdnNsdXR0IiwKCQkJCQkiZmFzaXQiOiAi\\nSmEiLAoJCQkJCSJ1dGZhbGwiOiAiS29tcGxla3N0IGJpbGRlIGhhciBiZXNr\\ncml2YW5kZSB0ZWtzdGFsdGVybmF0aXYuIgoJCQkJfSwKCQkJCSJuZWkiOiB7\\nCgkJCQkJInR5cGUiOiAiYXZzbHV0dCIsCgkJCQkJImZhc2l0IjogIk5laSIs\\nCgkJCQkJInV0ZmFsbCI6ICJCaWxkZSBzb20gZXIga29tcGxla3N0IG9nIHNv\\nbSBoYXIgZWl0IGxhbmd0IHRla3N0YWx0ZXJuYXRpdiBzb20gaWtramUgZ2ly\\nIGVpbiB1dGZ5bGxhbmRlIHNraWxkcmluZyBhdiBpbm5oYWxkZXQgaSBiaWxk\\nZS4iCgkJCQl9CgkJCX0KCQl9CgldCn0="
    val decoded =Base64.getDecoder().decode(data.replace("\\n","").toByteArray(Charset.forName("UTF-8")))
    val jsonString = String(decoded, Charset.defaultCharset())
    println(jsonString)


  }
}
