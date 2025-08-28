/*
 * This file is part of the vboard distribution.
 * (https://github.com/sncf-connect-tech/vboard)
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


angular.module('vboard').controller('VboardTeamProfilController', function VboardTeamProfilController($routeParams, $scope, vboardAuth, vboardMessageInterceptor, vboardImgs, $timeout, $rootScope, $location, $window, $http, CONFIG) {

    $scope.name = $routeParams.teamName;
    $scope.defaultAvatar = "images/avatar.png";
    $scope.focus = false;
    // Reactivate the search by saved labels
    $rootScope.disableFavoriteLabels  = false;
    // Hide the search toolbar
    $rootScope.hideSearchField();

    // List of localisations
    $scope.localisations = CONFIG.localisations.map((loc) => loc.id);
    $scope.localisationName = function (localisation) {
        return CONFIG.localisations.find(function (loc) {
            return loc.id === localisation;
        }).name;
    };

    // Check if the current user is on the team, and so if he can update the team info (based on user behavior as anyone can add themselves to a team)
    $scope.initPermission = function (team) {
        $scope.perm = team.members.includes(`${ $rootScope.userAuthenticated.first_name  };${  $rootScope.userAuthenticated.last_name  };${  $rootScope.userAuthenticated.email }`);
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
        if (success.members.length === 1 && success.members[0].length === 0) {
            $scope.team.members = [];
        }
        $scope.retrieveTeamAvatarBase64();
        $scope.profileResize();
    }, function (error) {
        vboardMessageInterceptor.showError(error, 'getTeam');
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

    /* eslint-disable-next-line angular/document-service */
    const personnalInfoDiv = document.getElementById('profil--personnal--info');
    const personnalInfoHeight = personnalInfoDiv.offsetHeight;

    // Text area and parent element resizing
    $scope.profileResize = function () {
        $timeout(function () {
            /* eslint-disable-next-line prefer-destructuring */
            const textarea = angular.element('.textArea-resize')[0];
            textarea.style.height = "45px";
            const isScrollBarShowing = (parseInt(textarea.style.height.slice(0, -2), 10) < textarea.scrollHeight);
            textarea.style.height = isScrollBarShowing ? `${ textarea.scrollHeight  }px`: textarea.style.height;
            const teamHeight = 65.6 * ($scope.team.members.length > 4 ? $scope.team.members.length -4: 0); // 65.6 is the size of the team element
            // textAreaSize: textarea size by default  -> textarea.scrollHeight - 40 = difference de la fenetre, - fileInputButton for the hidden buttons size and drop zone of upload files
            const heightCorrection = 100;
            personnalInfoDiv.style.height = `${ (personnalInfoHeight - heightCorrection + parseInt(textarea.style.height.slice(0, -2), 10)) + teamHeight  }px`;
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
        if ($location.url().includes("/profil/team") && $scope.focus) {
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
            $scope.myCroppedAvatar = ($scope.team.avatar && $scope.team.avatar !== true && $scope.team.avatar !== false) ? `data:image/png;base64,${ $scope.team.avatar }` : $scope.defaultAvatar;
        }
    });

    // Delete the team's avatar (confirmation popin can be added)
    $scope.submitDeleteAvatar = function () {
        $scope.imageSaved = false;
        $scope.team.avatar = "default";
        $scope.submitTeamUpdate($scope.team);
        $scope.myCroppedAvatar = $scope.defaultAvatar;
    };

    $scope.handleAvatarSelect = function (event) {
        // Display cropping element
        $scope.showCrop = true;
        $scope.imageSaved = false;
        /* eslint-disable-next-line prefer-destructuring */
        const file = event.currentTarget.files[0];
        const reader = new FileReader();
        reader.onload = function (evt) {
            /* eslint-disable no-shadow */
            $scope.$apply(function ($scope) {
                $scope.myAvatar = evt.target.result;
            });
        };
        reader.readAsDataURL(file);
    };

    $scope.viewValue = function (member) {
        return `${ member.split(';')[0]  } ${  member.split(';')[1] }`;
    };

    $scope.userSuggest = [];
    // Get the list of all VBoard users
    $scope.getUsers = function () {
        $http.get(`${ CONFIG.apiEndpoint  }/users/getAll/`).then(function (response) {
            if (response.status !== 200) {
                throw new Error(`User search failed:${  JSON.stringify(response) }`);
            }
            for (const index in response.data) {
                const user = `${ response.data[index].first_name  };${  response.data[index].last_name  };${  response.data[index].email }`;
                if (!_.contains($scope.team.members, user)) {
                    $scope.userSuggest.push(user);
                }
            }
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'getUsers');
        });
    };

    $scope.addTeam = function () {
        for (let index = $scope.team.members.length-1; index >= 0; index--) {
            if ($scope.team.members[index] === "Nouveau;Membre;@" && $scope.team.members.length > 1) {
                $scope.team.members.splice(index, 1);
            }
        }
        $scope.team.members.push("Nouveau;Membre;@");
        $scope.profileResize();
    };

    $scope.setMembers = function (val) {
        if (val.includes("Nouveau;Membre;@")) {
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
            /* eslint-disable-next-line angular/document-service */
            $scope.team.latitude = event.offsetY ? (event.offsetY) : event.pageY - document.getElementById("imglocalisation").offsetTop;
            /* eslint-disable-next-line angular/document-service */
            $scope.team.longitude = event.offsetX ? (event.offsetX) : event.pageX - document.getElementById("imglocalisation").offsetLeft;
        }
    };

    /** Gamification */
    $http.get(`${ CONFIG.apiEndpoint  }/gamification/getBadges/team/${  $scope.name }`).then(function (response) {
        if (response.status !== 200) {
            throw new Error(`Badges search failed:${  JSON.stringify(response) }`);
        }
        $scope.badges = response.data;
    }, function (error) {
        vboardMessageInterceptor.showError(error, 'VboardTeamProfilController');
    });

    $http.get(`${ CONFIG.apiEndpoint  }/gamification/getTeamStatsPercentage/${  $scope.name }`).then(function (response) {
        if (response.status !== 200) {
            throw new Error(`StatsPercentage search failed:${  JSON.stringify(response) }`);
        }
        $scope.statsPercentage = response.data;
    }, function (error) {
        vboardMessageInterceptor.showError(error, 'VboardTeamProfilController');
    });

});