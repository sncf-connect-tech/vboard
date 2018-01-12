package com.vsct.vboard.controllers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by lucas_cimon on 11/01/2018.
 */
@RestController
@RequestMapping("/version")
public class VersionController {
    @Value("${application.name}")
    private String name;
    @Value("${application.version}")
    private String version;
    @Value("${application.buildDate}")
    private String buildDate;

    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody Version getVersion() throws Exception {
        return new Version(name, version, buildDate);
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    static class Version {
        public String name;
        public String version;
        public String buildDate;

        public Version(String name, String version, String buildDate) {
            this.name = name;
            this.version = version;
            this.buildDate = buildDate;
        }
    }
}
