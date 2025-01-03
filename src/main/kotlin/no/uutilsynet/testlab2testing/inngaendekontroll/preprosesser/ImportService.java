package no.uutilsynet.testlab2testing.inngaendekontroll.preprosesser;


import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.CreateTestResultat;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomAttr;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImportService {

    final Logger logger = LoggerFactory.getLogger(ImportService.class);

    public List<ExtractedElement> getElements(String xpathExpression, URL url) {
        try (final WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            final HtmlPage page = webClient.getPage(url);

            final List<HtmlElement> elements = page.getByXPath(xpathExpression);

           return parseHtmlElements(elements);


        } catch (IOException e) {
            logger.error("Error while fetching elements", e);
        }


        return new ArrayList<>();
    }
    public List<ExtractedElement> parseHtmlElements(List<HtmlElement> elements) {
        return elements
                .stream()
                .map(ImportService::htmlElementToExtractedElement)
                .toList();

    }

    @NotNull
    private static ExtractedElement htmlElementToExtractedElement(HtmlElement element) {
        String tagName = element.getTagName();
        String text = element.getTextContent();
        String asXml = element.asXml();
        Map<String, String> attributes = toSimpleMap(element.getAttributesMap());
        String canonicalXPath = element.getCanonicalXPath();
        return new ExtractedElement(tagName, text, asXml, attributes, canonicalXPath);
    }

    private static Map<String,String> toSimpleMap(Map<String, DomAttr> attributes) {
        Map<String, String> resultMap = new HashMap<>();

        var entries = attributes.entrySet();
        entries.forEach(e -> resultMap.put(e.getKey(), e.getValue().getValue()));

        return resultMap;
    }

    public CreateTestResultat createTestElement(TestresultatBase testresultatBase, ExtractedElement element) {

        return new CreateTestResultat(testresultatBase.testgrunnlagId(),
                testresultatBase.loeysingId(),
                testresultatBase.testregelId(),
                testresultatBase.sideutvalId(),
                null,
                getElementName(element),
                element.asXml(),
                element.canonicalXPath(),
                null,
                null,Instant.now(),null);
    }


    private static String getElementName(ExtractedElement element) {
        if(element.attributes().containsKey("aria-describedby")) {
            return element.attributes().get("aria-describedby");
        }
        if(element.attributes().containsKey("aria-description")) {
            return element.attributes().get("aria-description");
        }
        if(element.attributes().containsKey("title")) {
            return element.attributes().get("title");
        }
        if(element.attributes().containsKey("alt")) {
            return element.attributes().get("alt");
        }
        return element.text();
    }
}
