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

angular.module('vboard').directive('vboardNotification', function () {
    return {
        restrict: 'E',
        scope: true,
        templateUrl: 'common/vboardNotification/templates/vboardNotification.html',
        controller: 'VboardNotificationController'
    }
});

angular.module('vboard').controller('VboardNotificationController', function ($scope, $http, $rootScope, CONFIG) {

    /** Retrieve the name of the notification's author */
    // Check whether the user format is valid (version compatibility)
    if ($scope.notification.from_user && $scope.notification.from_user.split(',').length > 2) {
        $scope.name = $scope.notification.from_user.split(',')[0] + ' ' + $scope.notification.from_user.split(',')[1];
        $scope.email = $scope.notification.from_user.split(',')[2];
    } else {
        $scope.name = $scope.notification.from_user;
        $scope.email = $scope.notification.from_user;
    }
    if ($scope.email === $rootScope.userAuthenticated.email) {
        $scope.name = 'Vous';
    }

    /** Notification icon */
    $scope.icon = function () {
        switch ($scope.notification.type) {
            case 'pin': return 'fi-book-bookmark t-color-black--text';
            case 'comment': return 'fi-comment t-color-black--text';
            case 'badge': return 'fi-unlock t-color-black--text';
            case 'role': return 'fi-torso-business t-color-black--text';
            default: return 'fi-flag t-color-black--text';
        }
    };

    // Notify the back-end that the notification has been clicked on, and set it as seen by the user (front)
    $scope.notificationClick = function () {
        $http.post(CONFIG.apiEndpoint + '/notifications/clicked/' + $scope.notification.id);
        $scope.notification.seen = true;
    };

});