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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class DBCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBCleaner.class);

    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
    public static void main(String[] args) throws SQLException {
        final String DB_URL = System.getenv().get("DB_URL");
        final String DB_NAME = System.getenv().get("DB_NAME");
        LOGGER.info("DB_URL={}", DB_URL);
        LOGGER.info("DB_NAME={}", DB_NAME);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP database " + DB_NAME);
                stmt.execute("CREATE database " + DB_NAME);
            }
        }
    }
}
