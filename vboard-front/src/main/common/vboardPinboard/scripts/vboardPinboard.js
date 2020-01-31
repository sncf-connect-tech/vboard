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

angular.module('vboard').directive('vboardPinboard', function vboardPinboard($timeout) {
    return {
        restrict: 'E',
        scope: {}, // Isolate scope
        templateUrl: 'common/vboardPinboard/templates/vboardPinboard.html',
        controller: 'VboardPinboardController',
        link(scope) {

            /** Create the unique packer */
            if (!scope.packer) {
                scope.packer = new Packery('.vboardPinboard', {
                    itemSelector: 'vboard-Pin',
                    columnWidth: 250,
                    gutter: 15
                });
            }

            scope.recomputeAfterImagesLoaded = function () {
                // From the docs: "The 'always' event is triggered after all images have been either loaded or confirmed broken"
                imagesLoaded('.vboardPinboard').on('always', function () {
                    // Note: imagesLoaded does not correctly detect this,
                    // and send this event a bit too early, so we add a slight delay
                    // until we find a better fix / replacement for it.
                    $timeout(() => {
                        scope.packer.reloadItems();
                        scope.packer.layout();
                    }, 2000);
                });
            };

            scope.recomputeAfterImagesLoaded();
        }
    }
});

angular.module('vboard').controller('VboardPinboardController', function VboardPinboardController($rootScope, $scope, $q, $timeout, $element, waitUntil, vboardPinsCollection) {

    $rootScope.disableFavoriteLabels = true;

    /** Show the search toolbar */
    $rootScope.showSearchField();

    /** Pin Management Service*/
    $scope.pinsCollection = vboardPinsCollection;

    /** Packery Object for displaying pins */
    $scope.packer = false;

    $scope.setPinsLikedAndSaved = function () {
        vboardPinsCollection.getUserLikes().then(function (success) {
            $scope.likes = success.data.map((like) => like.pin);
        });
        vboardPinsCollection.getUserSavedPins().then(function (success) {
            $scope.saved = success.data.map((pin) => pin.pin_id);
        });
    };

    /** After the pins loading, the panel is re-displayed */
    $scope.$on('vboardPinsCollectionUpdated', function () {
        $scope.recomputeAfterImagesLoaded();
    });

    /** Load other pins if scroll is down */
    $scope.$on('pinboardBottomReached', function () {
        $scope.pinsCollection.fetchMore();
    });

    /** Scroll scroll : 0 */
    $scope.$on('pinboardTopReached', function () {
        $scope.pinsCollection.fetchNoMore();
    });

    $rootScope.$watch('userAuthenticated', function () {
        if ($rootScope.userAuthenticated) {
            $scope.setPinsLikedAndSaved();
        }
    });

});