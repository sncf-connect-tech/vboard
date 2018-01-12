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

module.exports = function (grunt, options) {
    return {
        index: {
            files: [
                {
                    expand: true,
                    cwd: 'src/main',
                    src: 'index.html',
                    dest: 'grunt-target'
                }
            ]
        },
        config: {
            files: [
                {
                    expand: true,
                    cwd: '.',
                    src: 'config.js',
                    dest: 'grunt-target/compile/scripts'
                }
            ]
        },
        mainStatics: {
            files: [
                {
                    expand: true,
                    cwd: 'src/main',
                    src: ['favicon.ico', 'deprecated_browser.html'],
                    dest: 'grunt-target'
                }
            ]
        },
        images: {
            files: [
                {
                    expand: true,
                    cwd: 'src/main/images',
                    src: '**/*.{gif,png,jpeg,jpg,svg}',
                    dest: 'grunt-target/images'
                }
            ]
        },
        icons: {
            files: [
                {
                    expand: true,
                    cwd: 'src/main/bower_components/foundation-icon-fonts',
                    src: ['foundation-icons.css', 'foundation-icons.eot', 'foundation-icons.svg', 'foundation-icons.ttf', 'foundation-icons.woff'],
                    dest: 'grunt-target/fi-icons'
                }
            ]
        }
    };
};
