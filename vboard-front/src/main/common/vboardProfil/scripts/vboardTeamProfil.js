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

angular.module('vboard').controller('VboardTeamProfilController', function ($routeParams, $scope, vboardAuth, vboardMessageInterceptor, vboardImgs, $timeout, $rootScope, $location, $window, $http, API_ENDPOINT, CONFIG) {

    $scope.name = $routeParams.teamName;
    $scope.defaultAvatar = "images/avatar.png";
    $scope.focus = false;
    // Reactivate the search by saved labels
    $rootScope.disableFavoriteLabels  = false;
    // Hide the search toolbar
    $rootScope.hideSearchField();

    // List of localisations
    $scope.localisations = CONFIG.localisations.map(function(a) {return a.id;});
    $scope.localisationName = function(localisation) {
        return CONFIG.localisations.find(function (loc) {
            return loc.id === localisation;
        }).name;
    };

    // Check if the current user is on the team, and so if he can update the team info (based on user behavior as anyone can add themselves to a team)
    $scope.initPermission = function(team) {
        $scope.perm = team.members.indexOf($rootScope.userAuthenticated.first_name + ';' + $rootScope.userAuthenticated.last_name + ';' + $rootScope.userAuthenticated.email) !== -1;
        if ($scope.perm) {
            $scope.getUsers();
        }
    };

    vboardAuth.getTeam($scope.name).then(function (success) {
        $scope.team = success;
        console.log($scope.team);
        if ($rootScope.userAuthenticated) {
            $scope.initPermission(success);
        } else {
            $timeout(function () {
                $scope.initPermission(success);
            }, 2000);
        }

        // When the array is empty, the server still get an array with one empty element
        if (success.members.length === 1 && success.members[0].length === 0) { $scope.team.members = []; }
        $scope.retrieveTeamAvatarBase64();
        $scope.profileResize();


    }, function (error) {
        console.log('error: ', error );
        vboardMessageInterceptor.showErrorMessage("La récupération des informations de l'équipe a échoué. (Status Code: " + error.status + ") Veuillez tenter de recharger la page");
    });


    // Retrieve the user's avatar if he has one (so not "default").
    $scope.retrieveTeamAvatarBase64  = function () {
        if ($scope.team.avatar && $scope.team.avatar !== "default") {
            vboardImgs.getAvatar($scope.name).then(function (success) {
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
            var teamHeight = 65.6 * ($scope.team.members.length > 4 ? $scope.team.members.length -4: 0); // 65.6 is the size of the team element
            // textAreaSize: textarea size by default  -> textarea.scrollHeight - 40 = difference de la fenetre, - fileInputButton for the hidden buttons size and drop zone of upload files
            var heightCorrection = 100;
            personnalInfoDiv.style.height = (personnalInfoHeight - heightCorrection + parseInt(textarea.style.height.slice(0, -2), 10)) + teamHeight + "px";
        }, 100);
    };

    /** Profile update */
    $scope.submitTeamUpdate = function (team) {
        $scope.focus = false;
        team.avatar = (team.avatar && team.avatar !== true && team.avatar !== false) ? team.avatar : "unchanged";
        vboardAuth.updateTeam(team)
            .then(function () {
                vboardMessageInterceptor.showSuccessMessage("Informations enregistrées");
            }, function (error) {
                console.log('Update User ERROR', error);
            });
    };

    // Allow modifications to be saved even if the user is still focused on an input and leave the page (classic saving is done when the user gets out of a focusing element
    $window.onbeforeunload = function () {
        // handle the exit event
        if ($location.url().indexOf("/profil/team") >= 0 && $scope.focus) {
            $scope.submitTeamUpdate($scope.team);
        }
    };

    // Method allowing to know if the user is updating information
    $scope.teamUpdateFocus = function () {
        $scope.focus = true;
    };

    // Update team's avatar
    $scope.submitAvatarUpdate = function () {
        // Force the avatar to not change (see the $watch on 'myCroppedImage')
        $scope.imageSaved = true;
        $scope.team.avatar = $scope.myCroppedAvatar.slice($scope.myCroppedAvatar.indexOf(',')+1);
        $scope.submitTeamUpdate($scope.team);
        $scope.showCrop = false;

    };

    $scope.$watch('myCroppedAvatar', function () {
        if ($scope.imageSaved) {
            /** With rezising when the cropping element disappear, there is a zoom on the image that was not the one choosen by the user
             The element just need to be refreshed */
            // Check whether the avatar is not the one in the DB (boolean) but the base64 string
            $scope.myCroppedAvatar = ($scope.team.avatar && $scope.team.avatar !== true && $scope.team.avatar !== false) ? "data:image/png;base64,"+$scope.team.avatar : $scope.defaultAvatar;
        }
    });

    // Delete the team's avatar (confirmation popin can be added)
    $scope.submitDeleteAvatar = function () {
        $scope.imageSaved = false;
        $scope.team.avatar = "default";
        $scope.submitTeamUpdate($scope.team);
        $scope.myCroppedAvatar = $scope.defaultAvatar;
    };

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

    $scope.viewValue = function (member) {
        return member.split(';')[0] + ' ' + member.split(';')[1];
    };

    $scope.userSuggest = [];
    // Get the list of all VBoard users
    $scope.getUsers = function () {
        $http.get(API_ENDPOINT + '/users/getAll/').then(function (response) {
            if (response.status !== 200) {
                throw new Error('User search failed:' + JSON.stringify(response));
            }
            for (var index in response.data) {
                var user = response.data[index].first_name + ';' + response.data[index].last_name + ';' + response.data[index].email;
                if (!_.contains($scope.team.members, user)) {
                    $scope.userSuggest.push(user);
                }
            }
        }, function(error) {
            vboardMessageInterceptor.showErrorMessage("La récupération des utilisateurs a échoué. (Status Code: " + error.status + ") Veuillez tenter de recharger la page");
            console.log('error: ', error );
        });
    };

    $scope.addTeam = function () {
        for (var i = $scope.team.members.length-1; i >= 0; i--) {
            if ($scope.team.members[i] === "Nouveau;Membre;@" && $scope.team.members.length > 1) {
                $scope.team.members.splice(i, 1);
            }
        }
        $scope.team.members.push("Nouveau;Membre;@");
        $scope.profileResize();
    };

    $scope.setMembers = function (val) {
        if (val.indexOf("Nouveau;Membre;@") !== -1 ) {
            val.splice(val.indexOf("Nouveau;Membre;@"), 1);
        }
        vboardAuth.setMembers($scope.name, val);
    };

    $scope.removeMember = function (member) {
        $scope.team.members.splice($scope.team.members.indexOf(member), 1);
        $scope.userSuggest.push(member);
        $scope.setMembers($scope.team.members);
    };

    $scope.setLocalisation = false;
    $scope.changeLocalisation = function () {
        $scope.setLocalisation = true;
    };

    $scope.finishSetLocalisation = function () {
        if ($scope.setLocalisation) {
            $scope.submitTeamUpdate($scope.team);
            $scope.setLocalisation = false;
        }
    };

    $scope.getCoords =  function (event) {
        if ($scope.setLocalisation && $scope.perm) {
            $scope.team.latitude = event.offsetY ? (event.offsetY) : event.pageY - document.getElementById("imglocalisation").offsetTop;
            $scope.team.longitude = event.offsetX ? (event.offsetX) : event.pageX - document.getElementById("imglocalisation").offsetLeft;
        }
    };

    /** Gamification */
    $http.get(API_ENDPOINT + '/gamification/getBadges/team/' + $scope.name).then(function (response) {
        if (response.status !== 200) {
            throw new Error('Badges search failed:' + JSON.stringify(response));
        }
        $scope.badges = response.data;
    }, function (error) {
        vboardMessageInterceptor.showErrorMessage("La récupération des badges a échoué. (Status Code: " + error.status + ')');
        console.log('error: ', error);
    });

    $http.get(API_ENDPOINT + '/gamification/getTeamStatsPercentage/' + $scope.name).then(function (response) {
        if (response.status !== 200) {
            throw new Error('StatsPercentage search failed:' + JSON.stringify(response));
        }
        $scope.statsPercentage = response.data;
    }, function (error) {
        vboardMessageInterceptor.showErrorMessage("La récupération des pourcentages de progression a échoué. (Status Code: " + error.status + ')');
        console.log('error: ', error);
    });

});