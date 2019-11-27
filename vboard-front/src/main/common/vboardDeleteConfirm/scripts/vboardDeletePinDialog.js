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

angular.module('vboard').directive('vboardDeletePin', function () {
    return {
        restrict: 'E',
        scope: true, // new child scope
        templateUrl: 'common/vboardDeleteConfirm/templates/vboardDeletePin.html',
        controller: 'VboardDeletePinDialogController'
    }
});

angular.module('vboard').controller('VboardDeletePinDialogController', function (vboardPinsCollection, $scope, $timeout, ngDialog) {

    // Pin Deletion validation
    $scope.submit = function () {
        vboardPinsCollection.deletePin($scope.pin.pinId)
            .then(function () {
                $scope.closeThisDialog('OK'); // Close the confirmation popin
            }, function (error) {
                $scope.error = error;
                console.error('deletePin ERROR', error);
                $timeout(() => $scope.closeThisDialog('KO'), 5000);
            });
    };

    $scope.delete = function () {
        var popin = ngDialog.open({
            template: 'common/vboardDeleteConfirm/templates/vboardDeletePinConfirm.html',
            controller: 'VboardDeletePinDialogController',
            scope: $scope
        });
        popin.closePromise.then(function (data) {
            if (data && data.value && data.value === 'OK') {
                ngDialog.open({
                    template: 'common/vboardDeletePinDialogOK/templates/vboardDeletePinDialogOK.html',
                    controller: 'VboardDeletePinDialogOKController'
                });
            }
        });
    };
});
