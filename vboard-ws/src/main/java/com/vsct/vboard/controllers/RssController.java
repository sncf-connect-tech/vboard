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

import com.rometools.rome.feed.rss.*;
import com.vsct.vboard.models.Pin;
import com.vsct.vboard.services.ElasticSearchClient;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.feed.AbstractRssFeedView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class RssController {
    private final ElasticSearchClient elsClient;

    @Value("${com.vsct.vboard.hostname}")
    private String publicHostname;

    @Autowired
    public RssController(ElasticSearchClient elsClient) {
        this.elsClient = elsClient;
    }

    @RequestMapping(value = "/rss", method = RequestMethod.GET, produces= MediaType.APPLICATION_XML_VALUE)
    public ModelAndView getFeedInRss(@RequestParam(value = "text", required = false) String text,
                                     @RequestParam(value = "labels", required = false) String labels) {
        final String from = DateTimeFormat.forPattern("YYYY-MM-dd").print(DateTime.now().minusDays(7));
        final List<Pin> pins = elsClient.searchPins(text, labels, from, 0);

        final ModelAndView mav = new ModelAndView();
        mav.setView(new RssViewer(publicHostname));
        mav.addObject("feedContent", pins);
        return mav;
    }

    public static class RssViewer extends AbstractRssFeedView {
        private final String publicHostname;

        public RssViewer(final String publicHostname) {
            super();
            this.publicHostname = publicHostname;
        }

        @Override
        public String getContentType() {
            return "text/html;charset=UTF-8";
        }

        @Override
        protected void buildFeedMetadata(final Map<String, Object> model,
                                         final Channel feed,
                                         final HttpServletRequest request) {
            feed.setTitle("V.Board");
            feed.setLink("http://" + publicHostname);
            feed.setDescription("Les dernières épingles de V.Board");
            super.buildFeedMetadata(model, feed, request);
        }

        @Override
        protected List<Item> buildFeedItems(final Map<String, Object> model,
                                            final HttpServletRequest request,
                                            final HttpServletResponse response) {
            return ((List<Pin>)model.get("feedContent")).stream().map(pin -> {
                final Item item = new Item();

                item.setTitle(pin.getPinTitle());
                item.setAuthor(pin.getAuthor());
                item.setPubDate(pin.getPostDate().toDate());

                item.setLink("http://" + publicHostname + "/#/?id=" + pin.getPinId());

                final Description desc = new Description();
                desc.setType("text/plain");
                desc.setValue(pin.getIndexableTextContent());
                item.setDescription(desc);

                final Guid guid = new Guid();
                guid.setValue(pin.getPinId());
                item.setGuid(guid);

                item.setCategories(pin.getLabelsAsList().stream().map(label -> {
                    final Category category = new Category();
                    category.setValue(label.substring(1)); // Removing #-prefix
                    return category;
                }).collect(Collectors.toList()));

                return item;
            }).collect(Collectors.toList());
        }
    }
}
