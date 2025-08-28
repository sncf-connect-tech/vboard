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

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Random;

import com.vsct.vboard.models.User;
import org.joda.time.DateTime;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import com.vsct.vboard.models.Label;
import com.vsct.vboard.models.Pin;

public class TestUtil {
    private static final long DUMMY_GENERATOR_SEED = 42L;

    // Always generate the same infinite sequence of labels
    public static Iterator<Label> dummyLabelGenerator() {
        return new Iterator<Label>() {
            private final Random random = new Random(DUMMY_GENERATOR_SEED);

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Label next() {
                return new Label(new BigInteger(130, random).toString(32));
            }
        };
    }

    // Always generate the same infinite sequence of pins
    public static Iterator<Pin> dummyPinGenerator() {
        return new Iterator<Pin>() {
            private final Random random = new Random(DUMMY_GENERATOR_SEED);

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Pin next() {
                String num = "10";
                return new Pin(num, "title", "http://" + num + ".com", 0, "-", "-", ""+num, "-", new DateTime(random.nextLong()).toString(), 0);
            }
        };
    }

    public static Iterator<User> dummyUserGenerator() {
        return new Iterator<User>() {
            private final Random random = new Random(DUMMY_GENERATOR_SEED);

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public User next() {
                return new User(random.toString(), "firstName", "lastName");
            }
        };
    }

    public static void createTestDB() {
        new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.HSQL)
                .build();
    }
}
