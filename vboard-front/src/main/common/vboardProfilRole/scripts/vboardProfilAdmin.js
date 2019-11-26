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

angular.module('vboard').directive('vboardProfilAdmin', function () {
    return {
        restrict: 'E',
        scope: {}, // Isolate scope
        templateUrl: 'common/vboardProfilRole/templates/vboardProfilAdmin.html',
        controller: 'VboardProfilAdmin'
    }
});

angular.module('vboard').controller('VboardProfilAdmin', function ($scope, $http, CONFIG, vboardAuth, vboardMessageInterceptor) {

    var adminInit = function () {

        $http.get(CONFIG.apiEndpoint + '/messages').then( function (success) {
            if (success.data !== null || success.data !== '') {
                $scope.message = success.data;
            }
        });

        // Get the list of all newsletters moderators
        vboardAuth.getRole('Newsletter').then(function (response) {
            for (var index in response.data) {
                var newsletterMember = response.data[index].first_name + ',' + response.data[index].last_name + ',' + response.data[index].email;
                $scope.newsletterMembers.push(newsletterMember);
            }
            // Get the list of all VBoard users
            $http.get(CONFIG.apiEndpoint + '/users/getAll/').then(function (response2) {
                if (response2.status !== 200) {
                    throw new Error('User search failed:' + JSON.stringify(response2));
                }
                for (var index2 in response2.data) {
                    var user = response2.data[index2].first_name + ',' + response2.data[index2].last_name + ',' + response2.data[index2].email;
                    if ($scope.newsletterMembers.indexOf(user) === -1) {
                        $scope.userSuggest.push(user);
                    }
                }
            }, function(error) {
                vboardMessageInterceptor.showError(error, 'VboardProfilAdmin');
            });
        }, function(error) {
            vboardMessageInterceptor.showError(error, 'VboardProfilAdmin');
            console.error(error);
        });

        // Get the list of all newsletters moderators
        vboardAuth.getRole('Moderateur').then(function (response) {
            for (var index in response.data) {
                var moderatorMember = response.data[index].first_name + ',' + response.data[index].last_name + ',' + response.data[index].email;
                $scope.moderatorMembers.push(moderatorMember);
            }
            // Get the list of all VBoard users
            $http.get(CONFIG.apiEndpoint + '/users/getAll/').then(function (response2) {
                if (response2.status !== 200) {
                    throw new Error('User search failed:' + JSON.stringify(response2));
                }
                for (var index2 in response2.data) {
                    var user = response2.data[index2].first_name + ',' + response2.data[index2].last_name + ',' + response2.data[index2].email;
                    if ($scope.moderatorMembers.indexOf(user) === -1) {
                        $scope.userSuggest2.push(user);
                    }
                }
            }, function(error) {
                vboardMessageInterceptor.showError(error, 'VboardProfilAdmin');
            });
        }, function(error) {
            vboardMessageInterceptor.showError(error, 'VboardProfilAdmin');
        });

    };

    $scope.updateMessage = function (message) {
        if (vboardAuth.isAdmin()) {
            if (message.active && message.content !== null && message.content !== '' && message.info !== null) {
                vboardMessageInterceptor.showMessageAdmin(message.type, message.content);
            } else {
                vboardMessageInterceptor.hideMessageAdmin();
            }
        }
    };

    $scope.changeMessage = function (message) {
        if (vboardAuth.isAdmin() && message.active) {
            if (message.content !== null && message.content !=='' && message.info !== null) {
                vboardMessageInterceptor.showMessageAdmin(message.type, message.content);
            } else {
                vboardMessageInterceptor.hideMessageAdmin();
                message.active = false;
            }
        }
    };

    /** Display the full name of a user */
    $scope.viewUser = function (user) {
        return user.split(',')[0] + ' ' + user.split(',')[1];
    };

    $scope.viewEmail = function (user) {
        return user.split(',')[2];
    };

    /** User in charge of the newsletter */
    $scope.newsletterMembers = [];

    /** User in charge of the overseeing vboard (with admins but moderators can be set without code modification */
    $scope.moderatorMembers = [];

    $scope.addNewsletterMember = function () {
        $scope.newsletterMembers.push("Moderateur,Newsletter,@");
    };

    $scope.addModeratorMember = function () {
        $scope.moderatorMembers.push("Moderateur,,@");
    };

    $scope.removeNewsletterMember = function (user) {
        $scope.newsletterMembers.splice($scope.newsletterMembers.indexOf(user), 1);
        vboardAuth.removeRole($scope.viewEmail(user), "Newsletter");
    };

    $scope.setNewsletterRole = function (user) {
        vboardAuth.addRole($scope.viewEmail(user), "Newsletter");
    };

    $scope.removeModeratorMember = function (user) {
        $scope.moderatorMembers.splice($scope.moderatorMembers.indexOf(user), 1);
        vboardAuth.removeRole($scope.viewEmail(user), "Moderateur");
    };

    $scope.setModeratorRole = function (user) {
        vboardAuth.addRole($scope.viewEmail(user), "Moderateur");
    };

    /** User that can be chosen to add a role to */
    $scope.userSuggest = [];
    $scope.userSuggest2 = [];

    adminInit();

});
