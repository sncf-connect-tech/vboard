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

module.exports = {
    js: {
        src: ['grunt-target/compile/scripts/*.js', '!grunt-target/compile/scripts/config.js', '!grunt-target/compile/scripts/keycloak.json'],
        dest: 'grunt-target/compile/scripts'
    },
    css: {
        src: ['grunt-target/compile/styles/*.css'],
        dest: 'grunt-target/compile/styles'
    }
};
