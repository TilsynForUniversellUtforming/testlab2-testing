package no.uutilsynet.testlab2.autotester.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
public class CrawlingController {

    @PostMapping(
            value = "/crawlNettstad",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CrawlingResults crawlSite(@Validated @RequestBody CrawlingParameter crawlingParameter) {
        List<String> results = Arrays.asList("https://www.uutilsynet.no/wcag-standarden/245-flere-mater-niva-aa/107", "https://www.uutilsynet.no/wcag-standarden/331-identifikasjon-av-feil-niva/116",
                "https://www.uutilsynet.no/wcag-standarden/254-bevegelsesaktivering-niva/151",
                "https://www.uutilsynet.no/regelverk/regelverk/266",
                "https://www.uutilsynet.no/wcag-standarden/1412-tekstavstand-niva-aa/146",
                "https://www.uutilsynet.no/om-oss/om-oss/251",
                "https://www.uutilsynet.no/wcag-standarden/242-sidetitler-niva/104");

        return new CrawlingResults(results);
    }

    @GetMapping(value = "/",produces = MediaType.APPLICATION_JSON_VALUE)
    public String testApp() {
        return "It's alive";
    }
}
