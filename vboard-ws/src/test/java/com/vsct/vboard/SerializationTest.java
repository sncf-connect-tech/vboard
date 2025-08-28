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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.vboard.parameterFormat.AddNewPinParams;
import com.vsct.vboard.models.Label;
import com.vsct.vboard.models.Pin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static com.vsct.vboard.TestUtil.*;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MainController.class)
@ActiveProfiles(profiles = "test")
public class SerializationTest {
    @Autowired
    private MappingJackson2HttpMessageConverter httpMsgConverter;
    @Autowired
    private ObjectMapper jsonMapper;

    @Test
    public void deserializeSerializedLabel() throws IOException {
        Label dummyLabel = dummyLabelGenerator().next();
        String jsonLabel = this.jsonMapper.writeValueAsString(dummyLabel);
        Label pojoLabel = this.jsonMapper.readValue(jsonLabel, Label.class);
        assertEquals(dummyLabel, pojoLabel);
    }

    @Test
    public void deserializeSerializedPin() throws IOException {
        Pin dummyPin = dummyPinGenerator().next();
        String jsonPin = this.jsonMapper.writeValueAsString(dummyPin);
        Pin pojoPin = this.jsonMapper.readValue(jsonPin, Pin.class);
        assertEquals(dummyPin, pojoPin);
    }

    @Test
    public void deserializeElsSource() throws IOException {
        String jsonPin = "{\"pin_id\":\"tweet-598713611012460544\",\"href_url\":\"https://twitter.com/bytesforall/status/598713611012460544\",\"labels\":\"#Netfreedom,@APC_News,@IFEX\",\"post_dateutc\":\"2015-05-26T15:13:20.475Z\",\"indexable_text_content\":\"\\\"How would you like this wrapped?\\\" https://t.co/Le1g2VLkxr #Netfreedom @APC_News @IFEX http://t.co/hw6pozn9DD\"}";
        this.jsonMapper.readValue(jsonPin, Pin.class);
    }

    @Test
    public void classesShouldBeSerialisableByJackson() {
        // FROM: http://www.asyncdev.net/2011/12/http-media-type-not-supported-exception/
        assertCanBeDeserialized(AddNewPinParams.class);
        assertCanBeSerialized(Pin.class);
    }

    private void assertCanBeSerialized(Class<?> classToTest) {
        String message = format("%s is not serialisable", classToTest.getSimpleName());
        assertThat(message, this.httpMsgConverter.canWrite(classToTest, MediaType.APPLICATION_JSON), is(true));
    }

    private void assertCanBeDeserialized(Class<?> classToTest) {
        String message = format("%s is not deserialisable", classToTest.getSimpleName());
        assertThat(message, this.httpMsgConverter.canRead(classToTest, MediaType.APPLICATION_JSON), is(true));
    }
}
