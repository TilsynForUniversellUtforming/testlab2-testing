package no.uutilsynet.testlab2testing.sideutval

import java.net.URL

fun interface SideutvalSti {
    fun sti(): String
}

class SideutvalStiUrl(val url: URL): SideutvalSti {

    override fun sti(): String {
        return url.toString()
    }
}

class SideutvalStiReferanse(val referanse: String) : SideutvalSti {
    override fun sti(): String {
        return referanse
    }
}