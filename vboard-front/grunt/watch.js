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
    index: {
        files: ['src/main/index.html'],
        tasks: ['compileAll'],
        options: {
            spawn: true,
            atBegin: true,
            interrupt: true,
            debounceDelay: 500
        }
    },
    mainStatics: {
        files: ['src/main/constants.js', 'src/main/favicon.ico', 'src/main/deprecated_browser.html'],
        tasks: ['copy:mainStatics'],
        options: {
            spawn: true,
            atBegin: true,
            interrupt: true,
            debounceDelay: 500
        }
    },
    htmlTemplates: {
        files: ['src/main/common/*/templates/*.html'],
        tasks: ['compileAll'],
        options: {
            spawn: true,
            atBegin: true,
            interrupt: true,
            debounceDelay: 500
        }
    },
    js: {
        files: ['src/main/common/**/scripts/**/*.js'],
        tasks: ['compileJs'],
        options: {
            spawn: true,
            atBegin: true,
            interrupt: true,
            debounceDelay: 500
        }
    },
    css: {
        files: ['src/main/common/**/styles/**/*.css'],
        tasks: ['compileCss'],
        options: {
            spawn: true,
            atBegin: true,
            interrupt: true,
            debounceDelay: 500
        }
    }
};
