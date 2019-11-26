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

angular.module('vboard').directive('vboardProfil', function () {
    return {
        restrict: 'E',
        scope: false,
        templateUrl: 'common/vboardProfil/templates/vboardProfil.html',
        controller: 'VboardProfilController'
    }
});


angular.module('vboard').controller('VboardProfilController', function ($scope, $rootScope, $timeout, $window, vboardPinsCollection, vboardAuth, $location, $element, $http, CONFIG, vboardImgs, vboardMessageInterceptor) {


    /** Init */

    $scope.user = "";
    $scope.focus = false;
    $scope.defaultAvatar = "images/avatar.png";
    // Reactivate the search by saved labels
    $rootScope.disableFavoriteLabels = false;
    // Hide the search toolbar
    $rootScope.hideSearchField();


    /** Personal information */

    // Retrieve the user's avatar if he has one (so not "default").
    $scope.retrieveUserAvatarBase64 = function () {
        if ($rootScope.userAuthenticated && $scope.user.avatar && $scope.user.avatar !== "default") {
            vboardImgs.getAvatar($rootScope.userAuthenticated.email).then(function (success) {
                $scope.myCroppedAvatar = success; // Base64 image encoded string of the image
            });
        } else {
            $scope.myCroppedAvatar = $scope.defaultAvatar;
        }
    };

    // Update the user
    $scope.submitUserUpdate = function (user) {
        $scope.focus = false;
        var userToSend = angular.copy($scope.submitTeamUpdate(user));
        // Check whether the avatar is not the one in the DB (boolean) but the base64 string
        userToSend.avatar = (user.avatar && user.avatar !== true && user.avatar !== false) ? user.avatar : "unchanged";
        userToSend.team = user.team ? user.team.join(",") : "";
        userToSend.info = user.info ? user.info : "";

        vboardAuth.updateUser(userToSend)
            .then(function () {
                vboardMessageInterceptor.showSuccessMessage("Informations enregistrées");
            }, function (error) {
                console.error('Update User ERROR', error);
            });
    };

    // Update the user's avatar
    $scope.submitAvatarUpdate = function () {
        // Force the avatar to not change (see the $watch on 'myCroppedImage')
        $scope.imageSaved = true;
        $scope.user.avatar = $scope.myCroppedAvatar.slice($scope.myCroppedAvatar.indexOf(',')+1);
        $scope.submitUserUpdate($scope.user);
        $scope.showCrop = false;

    };

    // Delete the user's avatar (confirmation popin can be added)
    $scope.submitDeleteAvatar = function () {
        $scope.imageSaved = false;
        $scope.user.avatar = "default";
        $scope.submitUserUpdate($scope.user);
        $scope.myCroppedAvatar = $scope.defaultAvatar;
        $timeout(function () {
            vboardMessageInterceptor.showInfoMessage("L'image peut mettre un peu de temps à être enlevée des épingles et commentaires (cache)");
        }, 500);
    };

    // Method allowing to know if the user is updating information
    $scope.userUpdateFocus = function () {
        $scope.focus = true;
    };

    // Allow modifications to be saved even if the user is still focused on an input and leave the page (classic saving is done when the user gets out of a focusing element)
    $window.onbeforeunload = function () {
        // handle the exit event
        if ($location.url() === "/profil" && $scope.focus) {
            $scope.submitUserUpdate($scope.user);
        }
    };

    $scope.$watch('myCroppedAvatar', function () {
        if ($scope.imageSaved) {
            /** With rezising when the cropping element disappear, there is a zoom on the image that was not the one choosen by the user
             The element just need to be refreshed */
            // Check whether the avatar is not the one in the DB (boolean) but the base64 string
            $scope.myCroppedAvatar = ($scope.user.avatar && $scope.user.avatar !== true && $scope.user.avatar !== false) ? "data:image/png;base64,"+$scope.user.avatar : $scope.defaultAvatar;
        }
    });

    var personnalInfoDiv = angular.element(document.querySelector('#profil--personnal--info'))[0];
    var personnalInfoHeight = personnalInfoDiv.offsetHeight;

    // Text area and parent element resizing
    $scope.profileResize = function () {
        $timeout(function () {
            var textarea = angular.element(document.querySelector(".textArea-resize"))[0];
            var teamHeight = 65.6 * ($scope.user.team.length -1); // 65.6 is the size of the info element
            textarea.style.height = "45px";
            // 5 is to have a little margin under the text (to allow g,q,j,y letters not to be cut)
            textarea.style.height = (parseInt(textarea.style.height.slice(0, -2), 10) < textarea.scrollHeight) ? (textarea.scrollHeight) + 5 + "px": textarea.style.height;
            if (textarea.scrollHeight > 100) {
                var heightCorrection = 100;
                personnalInfoDiv.style.height = (personnalInfoHeight - heightCorrection + parseInt(textarea.style.height.slice(0, -2), 10)) + teamHeight + "px";
            } else {
                personnalInfoDiv.style.height = personnalInfoHeight + teamHeight + "px";
            }
        }, 100);
    };

    // Submit all the teams to the back-end
    $scope.submitTeamUpdate = function (user) {
        for (var i = user.team.length-1; i >= 0; i--) {
            // If the field team is empty and if there are another team already set, the field is removed
            if (!user.team[i] && user.team.length > 1) {
                user.team.splice(i, 1);
            }
            if (user.team[i]) {
                vboardAuth.addTeam(user.team[i]);
            }
        }
        $scope.profileResize();
        return user;
    };

    // Add a new field to put a team
    $scope.addTeam = function () {
        for (var i = $scope.user.team.length-1; i >= 0; i--) {
            if (!$scope.user.team[i] && $scope.user.team.length > 1) {
                $scope.user.team.splice(i, 1);
            }
        }
        // Prevent user to have more than 5 teams (only front-end restriction)
        if ($scope.user.team.length < 5) {
            $scope.user.team.push("");
            $scope.profileResize();
        } else {
            vboardMessageInterceptor.showWarningMessage("Vous appartenez à un peu trop d'équipes");
        }
    };

    // List of teams
    $http.get(CONFIG.apiEndpoint + '/users/teams').then(function (response) {
        if (response.status !== 200) {
            throw new Error('User search failed:' + JSON.stringify(response));
        }
        $scope.teamSuggest = response.data;
    }, function (error) {
        console.error('error: ', error);
    });


    var handleAvatarSelect = function (event) {
        // Display cropping element
        $scope.showCrop = true;
        $scope.imageSaved = false;
        var file = event.currentTarget.files[0];
        var reader = new FileReader();
        reader.onload = function (evt) {
            /* eslint-disable no-shadow */
            $scope.$apply(function ($scope) {
                $scope.myAvatar = evt.target.result;
            });
        };
        reader.readAsDataURL(file);
    };
    angular.element(document.querySelector('#avatarInput')).on('change', handleAvatarSelect);

    // Used for user to follow some labels
    $scope.getAllLabels = function () {
        vboardPinsCollection.getEveryLabels().then(function (allLabelsObject) {
            var allLabels = [];
            for (var labelValue in allLabelsObject) {
                allLabels.push(allLabelsObject[labelValue].label_name);
            }
            allLabels = allLabels.sort(); // Sort by special characters, then by Capital letters, then by lower-case letters
            if ($scope.user.favorite_labels && $scope.user.favorite_labels !== '') {
                var labels = $scope.user.favorite_labels.split(',');
                $scope.favoriteLabels = labels.sort();
                // Put the user's favorite label in one table and the other in another one
                for (var i = 0; i < $scope.favoriteLabels.length; i++) {
                    // So elements already in user's favorite labels are removed
                    if (allLabels.indexOf($scope.favoriteLabels[i]) > -1) {
                        allLabels.splice(allLabels.indexOf($scope.favoriteLabels[i]), 1);
                    }
                }
            }
            $scope.allLabels = allLabels;
        });
    };

    /** Labels */
    $scope.addLabel = function(label) {
        if ($scope.allLabels.indexOf(label) > -1) {
            $scope.allLabels.splice($scope.allLabels.indexOf(label), 1);
        }
        if (!$scope.favoriteLabels) {
            $scope.favoriteLabels = [];
        }
        $scope.favoriteLabels.push(label);
        $scope.favoriteLabels.sort();
        $scope.saveFavoriteLabels();
    };

    $scope.removeLabel = function(label) {
        if ($scope.favoriteLabels.indexOf(label) > -1) {
            $scope.favoriteLabels.splice($scope.favoriteLabels.indexOf(label), 1);
        }
        $scope.allLabels.push(label);
        $scope.allLabels.sort();
        $scope.saveFavoriteLabels();
    };

    $scope.saveFavoriteLabels = function () {
        if ($rootScope.userAuthenticated) {
            var labels = $scope.favoriteLabels;
            labels = labels.join(',');
            /* eslint-disable camelcase */
            $rootScope.userAuthenticated.favorite_labels = labels;
            $http.post(CONFIG.apiEndpoint + '/users/favoriteLabels', {
                labels: labels
            }).then(function (response) {
                if (response.status !== 200) {
                    throw new Error('Favorite Labels save failed:' + JSON.stringify(response));
                }
            }, function (error) {
                vboardMessageInterceptor.showError(error, 'saveFavoriteLabels');
            });
        }
    };

    /** Gamification */
    var gamification = function () {
        $http.get(CONFIG.apiEndpoint + '/gamification/getBadges').then(function (response) {
            if (response.status !== 200) {
                throw new Error('Badges search failed:' + JSON.stringify(response));
            }
            $scope.badges = response.data;
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'gamification');
        });

        $http.get(CONFIG.apiEndpoint + '/gamification/getStats').then(function (response) {
            if (response.status !== 200) {
                throw new Error('Stats search failed:' + JSON.stringify(response));
            }
            $scope.stats = response.data;
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'gamification');
        });

        $http.get(CONFIG.apiEndpoint + '/gamification/getStatsPercentage').then(function (response) {
            if (response.status !== 200) {
                throw new Error('StatsPercentage search failed:' + JSON.stringify(response));
            }
            $scope.statsPercentage = response.data;
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'gamification');
        });
    };

    $rootScope.$watch('userAuthenticated', function() {
        if ($rootScope.userAuthenticated) {
            // Retrieve the user in the DB
            vboardAuth.getUser($rootScope.userAuthenticated).then(function (success) {
                $scope.user = success;
                $scope.retrieveUserAvatarBase64();
                $scope.profileResize();
                $scope.getAllLabels();
                gamification();
                $scope.user.team = success.team.split(',');
                if (vboardAuth.isAdmin()) {
                    $scope.isAdmin = true;
                }
            });
        }
    });

});
