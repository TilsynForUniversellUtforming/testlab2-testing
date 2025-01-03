package no.uutilsynet.testlab2testing.inngaendekontroll.preprosesser;

import java.util.Map;

public record ExtractedElement(String tagName, String text, String asXml, Map<String, String> attributes, String canonicalXPath) {
}
