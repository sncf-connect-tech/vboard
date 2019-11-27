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

angular.module('vboard').directive('vboardLeaderBoard', function () {
    return {
        restrict: 'E',
        scope: true, // new child scope
        templateUrl: 'common/vboardLeaderBoard/templates/vboardLeaderBoard.html',
        controller: 'VboardLeaderBoardController'
    };
});

angular.module('vboard').controller('VboardLeaderBoardController', function ($rootScope, $scope, $http, CONFIG, vboardMessageInterceptor) {

    /** Hide the search toolbar */
    $rootScope.hideSearchField();
    // Reactivate the search by saved labels
    $rootScope.disableFavoriteLabels = false;
    var defaultAvatar = "images/avatar.png";

    /** Get the userLeaderBoard */
    $scope.userLeaderBoard = function () {
        $scope.teamStat = false;
        $http.get(CONFIG.apiEndpoint + '/gamification/getLeaders').then(function (response) {
            if (response.status !== 200) {
                throw new Error('Leaders search failed:' + JSON.stringify(response));
            }
            // Set the avatar and points (gamification points) of the leaders
            $scope.setAllAvatarImagesAndPoints(response.data);

        }, function (error) {
            vboardMessageInterceptor.showError(error, 'userLeaderBoard');
        });
    };

    /** Get the teamLeaderBoard */
    $scope.teamLeaderBoard = function () {
        $scope.teamStat = true;
        $scope.leaders = null;
        $http.get(CONFIG.apiEndpoint + '/gamification/getLeaders/teams').then(function (response) {
            if (response.status !== 200) {
                throw new Error('Leaders search failed:' + JSON.stringify(response));
            }
            // Set the avatar and points (gamification points) of the leaders
            $scope.setAllAvatarImagesAndPoints(response.data);
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'teamLeaderBoard');
        });
    };

    // Display the userLeaderBoard by default
    $scope.userLeaderBoard();


    $scope.displayName = function (leader) {
        return leader.name ? leader.name : leader.first_name + ' ' + leader.last_name;
    };

    // Set the avatar and points (gamification points) of the leaders
    $scope.setAllAvatarImagesAndPoints = function(stats) {

        // Action done for each section (pins posted, likes posted and receive, ...)

        stats.pins_posted.forEach(function(profil) {
            $scope.setAvatarImage(profil);
            $scope.setPoints(profil);
        });

        stats.likes_received.forEach(function(profil) {
            $scope.setAvatarImage(profil);
            $scope.setPoints(profil);
        });

        stats.likes_received_for_one_pin.forEach(function(profil) {
            $scope.setAvatarImage(profil);
            $scope.setPoints(profil);
        });

        stats.likes_posted.forEach(function(profil) {
            $scope.setAvatarImage(profil);
            $scope.setPoints(profil);
        });

        stats.comment_received.forEach(function(profil) {
            $scope.setAvatarImage(profil);
            $scope.setPoints(profil);
        });

        stats.comments_received_for_one_pin.forEach(function(profil) {
            $scope.setAvatarImage(profil);
            $scope.setPoints(profil);
        });

        stats.comments_posted.forEach(function(profil) {
            $scope.setAvatarImage(profil);
            $scope.setPoints(profil);
        });

        stats.connexions.forEach(function(profil) {
            $scope.setAvatarImage(profil);
            $scope.setPoints(profil);
        });

        $scope.leaders = stats;
    };

    // Set the avatar src of the leaders
    $scope.setAvatarImage = function (profil) {
        if (profil && profil.avatar) {
            profil.avatar = defaultAvatar;
            if (profil.name) {
                profil.avatar = "/avatar/" + profil.name + ".png";
            } else if (profil.email) {
                profil.avatar = "/avatar/" + profil.email + ".png";
            }
        } else {
            profil.avatar = defaultAvatar;
        }
    };

    // Set the points of the leaders
    $scope.setPoints = function (profil) {
        if (profil) {
            var serviceUrl = null;
            if (profil.name) {
                serviceUrl = CONFIG.apiEndpoint + '/gamification/getPoints/team/' + profil.name;
            } else if (profil.email) {
                serviceUrl = CONFIG.apiEndpoint + '/gamification/getPoints/' + profil.email;
            }

            if (serviceUrl) {
                $http.get(serviceUrl).then(function (response) {
                    if (response.status !== 200) {
                        throw new Error('Stats search failed:' + JSON.stringify(response));
                    }
                    profil.points = response.data;
                }, function (error) {
                    vboardMessageInterceptor.showError(error, 'setPoints');
                });
            } else {
                console.warn("Le profil" + profil + " n'a ni nom ni email permettant de retrouver ses points");
            }
        }
    };

});
