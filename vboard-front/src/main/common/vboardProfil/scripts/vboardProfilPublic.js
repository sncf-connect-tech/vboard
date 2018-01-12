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

angular.module('vboard').controller('VboardProfilPublicController', function ($scope, $rootScope, $routeParams, $timeout, $http, vboardPinsCollection, vboardAuth, vboardImgs, API_ENDPOINT, vboardMessageInterceptor) {

    $scope.email = $routeParams.email;
    $scope.public = true;
    $scope.defaultAvatar = "images/avatar.png";
    // Reactivate the search by saved labels
    $rootScope.disableFavoriteLabels  = false;
    // Hide the search toolbar
    $rootScope.hideSearchField();

    vboardAuth.getUserByEmail($scope.email).then(function (success) {
        $scope.user = success;
        $scope.getAvatar();
        $scope.profileResize();
        $scope.user.team = success.team.split(',');

    }, function (error) {
        console.log('error: ', error );
        $scope.user.email = $scope.email;
    });

    // Retrieve the user's avatar if he has one (so not "default").
    $scope.getAvatar = function () {
        if ($scope.user.avatar && $scope.user.avatar !== "default") {
            vboardImgs.getAvatar($scope.email).then(function (success) {
                $scope.myCroppedAvatar = success; // Base64 image encoded string of the image
            });
        } else {
            $scope.myCroppedAvatar = $scope.defaultAvatar;
        }
    };

    var personnalInfoDiv = angular.element(document.querySelector('#profil--personnal--info'))[0];
    var personnalInfoHeight = personnalInfoDiv.offsetHeight;

    // Text area and parent element resizing
    $scope.profileResize = function () {
        $timeout(function () {
            var textarea = angular.element(document.querySelector(".textArea-resize"))[0];
            textarea.style.height = "45px";
            var isScrollBarShowing = (parseInt(textarea.style.height.slice(0, -2), 10) < textarea.scrollHeight);
            textarea.style.height = isScrollBarShowing ? (textarea.scrollHeight) + "px": textarea.style.height;
            var teamHeight = 65.6 * ($scope.user.team.length -1); // 65.6 is the size of the info element
            // textAreaSize: textarea size by default  -> textarea.scrollHeight - 40 = difference de la fenetre, - fileInputButton for the hidden buttons size and drop zone of upload files
            var heightCorrection = 100;
            personnalInfoDiv.style.height = (personnalInfoHeight - heightCorrection + parseInt(textarea.style.height.slice(0, -2), 10)) + teamHeight + "px";
        }, 100);
    };

    /** Gamification */
    $http.get(API_ENDPOINT + '/gamification/getBadges/' + $scope.email).then(function (response) {
        if (response.status !== 200) {
            throw new Error('Badges search failed:' + JSON.stringify(response));
        }
        $scope.badges = response.data;
    }, function (error) {
        vboardMessageInterceptor.showErrorMessage("La récupération des badges a échoué. (Status Code: " + error.status + ')');
        console.log('error: ', error);
    });

    $http.get(API_ENDPOINT + '/gamification/getStats/' + $scope.email).then(function (response) {
        if (response.status !== 200) {
            throw new Error('Stats search failed:' + JSON.stringify(response));
        }
        $scope.stats = response.data;
    }, function (error) {
        vboardMessageInterceptor.showErrorMessage("La récupération des stats a échoué. (Status Code: " + error.status + ')');
        console.log('error: ', error);
    });

    $http.get(API_ENDPOINT + '/gamification/getStatsPercentage/' + $scope.email).then(function (response) {
        if (response.status !== 200) {
            throw new Error('StatsPercentage search failed:' + JSON.stringify(response));
        }
        $scope.statsPercentage = response.data;
    }, function (error) {
        vboardMessageInterceptor.showErrorMessage("La récupération des barres de progression a échoué. (Status Code: " + error.status + ')');
        console.log('error: ', error);
    });

});