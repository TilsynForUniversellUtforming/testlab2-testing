package no.uutilsynet.testlab2testing.inngaendekontroll.preprosesser;

import org.htmlunit.html.DomAttr;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.println;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ImportServiceTest {

    @Autowired
    ImportService importService;

    final Logger logger = LoggerFactory.getLogger(ImportServiceTest.class);



    @Test
    void getElements() throws MalformedURLException {

        var elements = importService.getElements("//img", URI.create("https://www.demoblaze.com/").toURL());

        var element = elements.getFirst();

        var attributes = element.attributes();

        attributes.forEach((k, v) -> logger.info("Key %s Value %s".formatted(k, v)));


        logger.info("Elements %d".formatted(elements.size()));


        println("Elements " + elements);

        assertFalse(elements.isEmpty());
    }

    @Test
    void createTestElementAriaDescribedByPreferedTitle() {

        var attributes = Map.of("aria-describedby", "aria-describedby","title", "title");

        var extractedElement = new ExtractedElement("div", "text", "<img aria-describedby='aria-describedby' title='title'></img>", attributes, "canonicalXPath");

        var testresultatBase = new TestresultatBase(1, 1, 1, 1);

        var result = importService.createTestElement(testresultatBase,extractedElement);

        assertEquals(1,result.getTestgrunnlagId());
        assertEquals("aria-describedby", result.getElementOmtale());
    }

    @Test
    void createTestElementTitle() {

        var attributes = Map.of("title", "title");

        var extractedElement = new ExtractedElement("div", "text", "<img aria-describedby='aria-describedby' title='title'></img>", attributes, "canonicalXPath");

        var testresultatBase = new TestresultatBase(1, 1, 1, 1);

        var result = importService.createTestElement(testresultatBase,extractedElement);

        assertEquals( 1,result.getTestgrunnlagId());
        assertEquals("title", result.getElementOmtale());
    }
}