package dev.camel.backendlab.scenario.nplus1.api;

import dev.camel.backendlab.scenario.nplus1.service.Nplus1Service;
import dev.camel.backendlab.scenario.nplus1.service.Nplus1Variant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/nplus1")
@RequiredArgsConstructor
public class Nplus1Controller {

    private final Nplus1Service nplus1Service;

    @GetMapping
    public Nplus1ResultResponse run(
        @RequestParam(defaultValue = "n-plus-one") String variant,
        @RequestParam(required = false) Integer authorCount
    ) {
        try {
            return nplus1Service.run(Nplus1Variant.from(variant), authorCount);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/compare")
    public Nplus1CompareResponse compare(@RequestParam(required = false) Integer authorCount) {
        return nplus1Service.compare(authorCount);
    }

    @GetMapping("/variants")
    public List<Nplus1ResultResponse.VariantOption> variants() {
        return nplus1Service.getVariantOptions();
    }
}

