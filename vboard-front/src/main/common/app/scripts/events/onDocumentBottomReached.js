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

'use strict';

// Emit an event when the bottom page is reached (to allow the app to load more pins)
angular.module('vboard').directive('onDocumentBottomReached', ['$window', '$document', '$interval', function ($window, $document, $interval) {
    return {
        restrict: 'A',
        link: function ($scope, $element, $attr) {
            var isWindowAtBottom = false,
                isWindowAtTop = false,
                hasEventAlreadyBeenSent = false,
                hasScrollBottomEventBeenTriggered = false,
                lastWindowScroll = 0,
                window = angular.element($window);
            window.bind("scroll", function () {
                // We execute as few code as possible in this callback because it can seriously slow down the user scrolling
                var body = document.body,
                    html = document.documentElement;

                var docHeight = Math.max( body.scrollHeight, body.offsetHeight,
                    html.clientHeight, html.scrollHeight, html.offsetHeight );

                isWindowAtBottom = ($window.pageYOffset + 50 >= (docHeight - $window.innerHeight));
                isWindowAtTop = ($window.pageYOffset < 500);
            });
            $interval(function () {
                if (isWindowAtBottom && lastWindowScroll !== $window.pageYOffset) {
                    // We use a second variable to only send the event once
                    if (!hasEventAlreadyBeenSent) {
                        hasEventAlreadyBeenSent = true;
                        hasScrollBottomEventBeenTriggered = true;
                        lastWindowScroll = $window.pageYOffset;
                        $scope.$emit($attr.onDocumentBottomReached);
                    }
                } else {
                    hasEventAlreadyBeenSent = false;
                }
                if (isWindowAtTop && hasScrollBottomEventBeenTriggered) {
                    // We use a second variable to only send the event once
                    if (!hasEventAlreadyBeenSent) {
                        hasEventAlreadyBeenSent = true;
                        hasScrollBottomEventBeenTriggered = false;
                        $scope.$emit($attr.onDocumentTopReached);
                    }
                } else {
                    hasEventAlreadyBeenSent = false;
                }
            }, 500);
        }
    }
}]);