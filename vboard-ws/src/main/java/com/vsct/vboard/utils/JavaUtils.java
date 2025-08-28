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

package com.vsct.vboard.utils;

import org.codehaus.jettison.json.JSONObject;

import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaUtils {

    public static String getRegexMatchGroup(Pattern pattern, String str) {
        Matcher matcher = pattern.matcher(str);
        if (matcher.groupCount() != 1) {
            throw new IllegalArgumentException("The regex pattern provided does not have only ONE capturing group: " + pattern.pattern());
        }
        if (!matcher.matches()) {
            throw new NoSuchElementException("The string '" + str + "' does not match the regex pattern:" + pattern.pattern());
        }
        return matcher.group(1);
    }

    public static String extractJSONObject(String object, String paramName) {
        try {
            JSONObject jsonObject = new JSONObject(object);
            object = jsonObject.getString(paramName);
        } catch (Exception e) {
            throw new IllegalArgumentException("Not able to cast " + object, e);
        }
        return object;
    }

}
