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
    options: {
        frameworks: ['jasmine'],
        browsers: ['PhantomJS'],
        files: [
            // bower:js
            'src/main/bower_components/jquery/dist/jquery.js',
            'src/main/bower_components/angular/angular.js',
            'src/main/bower_components/angular-animate/angular-animate.js',
            'src/main/bower_components/angular-aria/angular-aria.js',
            'src/main/bower_components/angular-sanitize/angular-sanitize.js',
            'src/main/bower_components/arrive/src/arrive.js',
            'src/main/bower_components/lodash/lodash.js',
            'src/main/bower_components/classie/classie.js',
            'src/main/bower_components/get-style-property/get-style-property.js',
            'src/main/bower_components/get-size/get-size.js',
            'src/main/bower_components/eventie/eventie.js',
            'src/main/bower_components/doc-ready/doc-ready.js',
            'src/main/bower_components/eventEmitter/EventEmitter.js',
            'src/main/bower_components/matches-selector/matches-selector.js',
            'src/main/bower_components/fizzy-ui-utils/utils.js',
            'src/main/bower_components/outlayer/item.js',
            'src/main/bower_components/outlayer/outlayer.js',
            'src/main/bower_components/packery/js/rect.js',
            'src/main/bower_components/packery/js/packer.js',
            'src/main/bower_components/packery/js/item.js',
            'src/main/bower_components/packery/js/packery.js',
            'src/main/bower_components/imagesloaded/imagesloaded.js',
            'src/main/bower_components/moment/moment.js',
            'src/main/bower_components/ngDialog/js/ngDialog.js',
            'src/main/bower_components/nsPopover/src/nsPopover.js',
            'src/main/bower_components/angular-route/angular-route.js',
            'src/main/bower_components/ngImgCropFullExtended/compile/minified/ng-img-crop.js',
            'src/main/bower_components/angular-cookies/angular-cookies.js',
            'src/main/bower_components/bootstrap/dist/js/bootstrap.js',
            'src/main/bower_components/angular-ui-select/dist/select.js',
            'src/main/bower_components/angular-mocks/angular-mocks.js',
            // endbower
            'src/main/common/**/*.js',
            'src/test/specs/**/*.js'
        ]
    },
    dev: {
        autoWatch: false,
        singleRun: true,
        logLevel: 'INFO'
    }
};
