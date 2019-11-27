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

angular.module('vboard').controller('VboardProfilSearchController', function ($rootScope, $scope, $http, CONFIG, vboardMessageInterceptor) {
// Reactivate the search by saved labels
    $rootScope.disableFavoriteLabels = false;
    // Hide the search toolbar
    $rootScope.hideSearchField();

    $scope.showUserLinkButton = false;
    $scope.showTeamLinkButton = false;

    /** Display the full name of a user */
    $scope.viewUser = function (user) {
        return user.split(',')[0] + ' ' + user.split(',')[1];
    };

    $scope.getEmail = function (user) {
        return user.split(',')[2];
    };

    $scope.user = 'Rechercher,utilisateur,@';
    $scope.userSuggest = [];
    // Get the list of all VBoard users
    $http.get(CONFIG.apiEndpoint + '/users/getAll/').then(function (response) {
        if (response.status !== 200) {
            throw new Error('User search failed:' + JSON.stringify(response));
        }
        response.data.forEach(function (userObject) {
            var user = userObject.first_name + ',' + userObject.last_name + ',' + userObject.email;
            $scope.userSuggest.push(user);
        });
    }, function(error) {
        vboardMessageInterceptor.showError(error, 'VboardProfilSearchController');
    });

    $scope.seeUserProfil = function (user) {
        $scope.user = user;
        $scope.showUserLinkButton = true;
    };

    $scope.team = 'Rechercher équipe';
    $scope.teamSuggest = [];
    // List of teams
    $http.get(CONFIG.apiEndpoint + '/users/teams').then(function (response) {
        if (response.status !== 200) {
            throw new Error('User search failed:' + JSON.stringify(response));
        }
        $scope.teamSuggest = response.data;
    }, function (error) {
        vboardMessageInterceptor.showError(error, 'VboardProfilSearchController');
    });

    $scope.seeTeamProfil = function (team) {
        $scope.team = team;
        $scope.showTeamLinkButton = true;
    };

});
