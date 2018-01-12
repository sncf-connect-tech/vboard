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

var moment = require('moment'),
    pkg = require('../package.json')    ;

module.exports = {
    admin_version: {
        options: {
            patterns: [
                {
                    match: 'version',
                    replacement: pkg.version
                },
                {
                    match: 'buildDate',
                    replacement: moment().format('YYYYMMDD_HHmm')
                }
            ]
        },
        files: [
            {src: 'src/main/admin-version.json', dest: 'grunt-target/admin-version.json'}
        ]
    }
};