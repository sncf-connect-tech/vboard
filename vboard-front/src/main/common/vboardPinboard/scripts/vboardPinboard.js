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

angular.module('vboard').constant('VboardPinsContainerClassname', '.vboardPinboard');

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
                scope.packer.reloadItems();
            }

            /** Recalculate the layout every time the directive is called */
            $timeout(function () {
                scope.packer.reloadItems();
                scope.packer.layout();
            }, 500);

            scope.refresh = function () {
                scope.packer.reloadItems();
                scope.packer.layout();
            };
        }
    }
});

angular.module('vboard').controller('VboardPinboardController', function VboardPinboardController($rootScope, $scope, $q, $timeout, $element, waitUntil, vboardPinsCollection, VboardPinsContainerClassname) {

    /** Display the layout after images finish loading */
    const recomputeAfterImagesLoaded = function () {
        imagesLoaded(VboardPinsContainerClassname, function () {
            $scope.refresh();
        });
    };

    /** Prevent bad display on late display */
    $timeout(function () {
        $scope.render($scope.pinsCollection.pins);
        $scope.packer.reloadItems();
        $scope.packer.layout();
    }, 2000);

    $rootScope.disableFavoriteLabels = true;

    /** Show the search toolbar */
    $rootScope.showSearchField();

    /** Pin Management Service*/
    $scope.pinsCollection = vboardPinsCollection;

    /** Packery Object for displaying pins */
    $scope.packer = false;

    /** Display the pin panel */
    $scope.render = function () {
        return waitUntil.elemCreated(VboardPinsContainerClassname).then(function () {
            // $scope.create = true; (Can be used to trigger function in the directive with a $watch)
            return recomputeAfterImagesLoaded();
        }).catch(function (error) {
            throw error;
        });
    };

    /** After the pins loading, the panel is re-displayed */
    $scope.$on('vboardPinsCollectionUpdated', function () {
        $scope.render($scope.pinsCollection.pins);
        $timeout(function () {
            $scope.packer.reloadItems();
            $scope.packer.layout();
        }, 500);
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
            /** Retrieve the user's likes */
            vboardPinsCollection.getUserLikes().then(function (success) {
                $scope.likes =[];
                success.data.forEach(function (like) {
                    // Put in scope.likes the pin's id liked by the current user
                    $scope.likes.push(like.pin);
                });
            });
            /** Retrieve the user's savedPins */
            vboardPinsCollection.getUserSavedPins().then(function (success) {
                $scope.saved = [];
                $scope.saved = _.map(success.data, 'pin_id'); // Put in scope.saved the pin's id saved by the current user
            });
        }
    });

});