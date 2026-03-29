package com.trainingsplan.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/changelog")
public class ChangelogController {

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String getChangelog() throws IOException {
        ClassPathResource resource = new ClassPathResource("CHANGELOG.md");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
