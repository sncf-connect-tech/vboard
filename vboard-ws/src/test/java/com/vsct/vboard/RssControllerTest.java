/*
 * This file is part of the vboard distribution.
 * (https://github.com/sncf-connect-tech/vboard)
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

package com.vsct.vboard;

import com.vsct.vboard.controllers.RssController;
import com.vsct.vboard.models.Pin;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RssControllerTest {

    private static final List<Pin> TEST_PINS = new ArrayList<Pin>() {{
        add(new Pin("vboard-1",
                "TOTO à la plage",
                "http://toto.a.la.plage",
                3,
                "http://toto.a.la.plage.png",
                "#toto,#plage",
                "Hier, toto est allé à la plage",
                "Toto",
                new DateTime("2017-06-13T17:38:55.795+02:00")));
    }};

    private static final String EXPECTED_XML_RSS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
            "<rss version=\"2.0\">\r\n" +
            "  <channel>\r\n" +
            "    <title>V.Board</title>\r\n" +
            "    <link>http://vboard.com</link>\r\n" +
            "    <description>Les dernières épingles de V.Board</description>\r\n" +
            "    <item>\r\n" +
            "      <title>TOTO à la plage</title>\r\n" +
            "      <link>http://vboard.com/#/?id=vboard-1</link>\r\n" +
            "      <description>Hier, toto est allé à la plage</description>\r\n" +
            "      <category>toto</category>\r\n" +
            "      <category>plage</category>\r\n" +
            "      <pubDate>Tue, 13 Jun 2017 15:38:55 GMT</pubDate>\r\n" +
            "      <author>Toto</author>\r\n" +
            "      <guid isPermaLink=\"false\">vboard-1</guid>\r\n" +
            "    </item>\r\n" +
            "  </channel>\r\n" +
            "</rss>\r\n";

    private String modelAndViewAsString(final ModelAndView mav) throws Exception {
        final MockHttpServletRequest mockReq = new MockHttpServletRequest();
        final MockHttpServletResponse mockResp = new MockHttpServletResponse();
        mav.getView().render(mav.getModel(), mockReq, mockResp);
        return mockResp.getContentAsString();
    }

    @Test
    public void getLikesFromAuthor() throws Exception {
        final ModelAndView mav = new ModelAndView();
        mav.setView(new RssController.RssViewer("vboard.com"));
        mav.addObject("feedContent", TEST_PINS);
        final String htmlView = modelAndViewAsString(mav);
        assertEquals(EXPECTED_XML_RSS, htmlView);
    }

}