/*
 * This file is part of the vboard distribution.
 * (https://github.com/voyages-sncf-technologies/vboard)
 * Copyright (c) 2017 VSCT.
 *
 * vboard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, version 3.
 *
 * vboard is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vsct.vboard.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.vboard.DAO.LabelDAO;
import com.vsct.vboard.DAO.PinDAO;
import com.vsct.vboard.models.Label;
import com.vsct.vboard.models.Pin;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

@RestController
@RequestMapping(value = "/labels")
public class LabelsController {
    private final ObjectMapper jsonMapper;
    private final JdbcTemplate jdbcTemplate;
    private final LabelDAO labelDAO;
    private final PinDAO pinDAO;

    @Autowired
    public LabelsController(ObjectMapper jsonMapper, JdbcTemplate jdbcTemplate, LabelDAO labelDAO, PinDAO pinDAO) {
        this.jsonMapper = jsonMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.labelDAO = labelDAO;
        this.pinDAO = pinDAO;
    }

    public void deleteAllLabels() {
        this.jdbcTemplate.execute("TRUNCATE TABLE labels;");
    }

    public List<Label> loadLabelsFromJson(InputStream jsonInput) {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(jsonInput, writer, "UTF-8");
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        String str = writer.toString();
        List<String> strLabels;
        try {
            strLabels = this.jsonMapper.readValue(str, new TypeReference<List<String>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Label> labels = strLabels.stream().map(Label::new)
                .collect(Collectors.toList());
        this.labelDAO.saveAll(labels);
        return labels;
    }

    // Return all labels still on any pins
    @RequestMapping(value = "/throughPins", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Set<Label> getAllFromPins() {
        Set<Label> labels = new HashSet<>();
        Iterable<Pin> pins = this.pinDAO.findAll();
        pins.forEach(p -> {
            if (!isBlank(p.getLabels())) {
                List<String> pinLabels = p.getLabelsAsList();
                pinLabels.forEach(l -> {
                    labels.add(new Label(l));
                    this.labelDAO.save(new Label(l));
                });
            }
        });
        return labels;
    }

    // Return any label once put on a pin
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Iterable<Label> getAll() {
        return this.labelDAO.findAll();
    }
}
