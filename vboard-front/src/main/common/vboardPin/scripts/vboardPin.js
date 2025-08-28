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


angular.module('vboard').directive('vboardPin', function vboardPin() {
    return {
        restrict: 'E',
        scope: true, // new child scope
        templateUrl: 'common/vboardPin/templates/vboardPin.html',
        controller: 'VboardPinController',
        link($scope) {

            /** pin's date of creation */
            $scope.creationDate = function (pin) {
                return moment(pin.postDateUTC).fromNow();
            };
            /** pin's labels */
            $scope.tags = function (pin) {
                return pin.labels ? pin.labels.split(',')  : '';
            };
            /** url encoding */
            $scope.encode = function (tag) {
                return encodeURIComponent(tag);
            }
        }
    };
});

angular.module('vboard').controller('VboardPinController', function VboardPinController($scope, $rootScope, vboardAuth, vboardImgs, vboardPinsCollection, ngDialog, $http, CONFIG, vboardMessageInterceptor, $sce) {

    $scope.authorLink = '';
    $scope.likesAuthors = "";
    $scope.newComment = false;
    $scope.commentInput = "";
    $scope.pinAvatar = "images/avatar.png";

    $scope.readableAuthor = $scope.pin.author;
    if ($scope.pin.author && $scope.pin.author.includes(',') && $scope.pin.author.includes('@')) {
        $scope.authorLink = `#profil/${  $scope.pin.author.split(',')[2] }`;
    }

    // Check whether the user is the author of the pin or an admin, so if the user can delete/update the pin.
    // Also check if the user is authenticated and if he has the newsletter role.
    $scope.setPerm = function () {
        $scope.setModificationPerm();
        $scope.connected = $rootScope.userAuthenticated;
        $scope.hasNewsletterRole = $rootScope.userAuthenticated ? $rootScope.userAuthenticated.roles.includes('Newsletter') : false;
    };

    $scope.setModificationPerm = function () {
        $scope.perm = $rootScope.userAuthenticated ? ((`${ $rootScope.userAuthenticated.first_name  },${  $rootScope.userAuthenticated.last_name  },${  $rootScope.userAuthenticated.email }`) === $scope.pin.author ||
        vboardAuth.isAdmin() || vboardAuth.isModerator() || $rootScope.userAuthenticated.email === $scope.pin.author /* version compatible code*/): false;
    };

    /** Media */

    $scope.loadImage = function () {
        if ($scope.pin.imgType && $scope.pin.imgType === 'custom') {
            // Value set to null to avoid the view to display 'custom'
            $scope.pin.imgType = null;
            vboardImgs.getPinImage($scope.pin.pinId).then(function (success) {
                $scope.pin.imgType = `data:image/png;base64,${  success }`; // Base64 string representation of the image
                vboardPinsCollection.sendBroadcastUpdate();
            });
        }
    };

    // If the link contains <iframe, the app put it as a vidéo (set videoLink)
    $scope.videoLink = "";
    if ($scope.pin.imgType && $scope.pin.imgType.startsWith('<iframe')) {
        const scr = $scope.pin.imgType.substring($scope.pin.imgType.indexOf('src') + 5);
        $scope.videoLink = $sce.trustAsResourceUrl(scr.substring(0, scr.indexOf('"'))); // trust the url, the iframe is recreating in html to avoid injection
    }


    /** Likes */

    $scope.addLike = function (pinId) {
        vboardPinsCollection.addLike(pinId).then(function () {
            $scope.likes.push(pinId); // Remove the like in front (when mouse over)
            $scope.pin.likes++; // Show the number of likes
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'addLike');
        });
    };

    $scope.unlike = function (pinId) {
        vboardPinsCollection.removeLike(pinId).then(function () {
            const index = $scope.likes.indexOf(pinId);
            if (index > -1) {
                $scope.likes.splice(index, 1); // Remove the like in front (when mouse over)
                $scope.pin.likes--; // Show the number of likes
            }
            // To prevent loading likes failed at the start and display 0
            if ($scope.pin.likes <= 0 ) {
                $scope.pin.likes = 0;
            }
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'unlike');
        });
    };

    $scope.seeLikes = function (pinId) {
        // Do not reload the likes if it has already been done (function called on mouseover)
        if ($scope.likesAuthors === "") {
            vboardPinsCollection.seeLikes(pinId).then(function (success) {
                $scope.likesAuthors = "";
                $scope.pin.likes = success.data.length;
                success.data.forEach(function (like) {
                    $scope.setLikesAuthors(like.author);
                });
            });
        }
    };


    /** Comment */

    // Actually show the section to add a comment (or hide it if it was already shown)
    $scope.addComment = function () {
        $scope.newComment = !$scope.newComment;
        if ($scope.newComment) {
            $scope.showComments();
        } else {
            $scope.hideComments();
        }
        vboardPinsCollection.sendBroadcastUpdate();
    };

    $scope.submitComment = function () {
        // For the data-ng-if, force the update of the last comment. (The last comment functionnality was dropped, but for version compatibility, the code remained)
        vboardPinsCollection.addComment($scope.commentInput, $scope.pin.pinId).then(function () {
            $scope.newComment = false;
            $scope.showAllComments = null;
            $scope.createAllComments = false;
            $scope.pin.commentsNumber++; // Show the number of comments
            vboardPinsCollection.sendBroadcastUpdate();
        });
        vboardPinsCollection.sendBroadcastUpdate();
    };

    $scope.hideComments = function () {
        $scope.showAllComments = false;
        vboardPinsCollection.sendBroadcastUpdate();
    };

    $scope.showComments = function () {
        if ($scope.showAllComments !== false) {
            vboardPinsCollection.getComments($scope.pin.pinId).then(function (success) {
                $scope.createAllComments = true;
                $scope.comments = success.data;
                vboardPinsCollection.sendBroadcastUpdate();
            });
        }
        $scope.showAllComments = true;
        vboardPinsCollection.sendBroadcastUpdate();
    };

    // Content to display on the tooltip of likes
    $scope.setLikesAuthors = function (email) {
        vboardAuth.getUserByEmail(email).then(function (success2) {
            if (!$scope.likesAuthors.includes(`${ success2.first_name  } ${ success2.last_name }`)) {
                if ($scope.likesAuthors) {
                    $scope.likesAuthors = `${ $scope.likesAuthors  }\n`
                }
                $scope.likesAuthors = `${ $scope.likesAuthors + success2.first_name  } ${  success2.last_name }`;
            }
        });
    };


    /** Pin Update */

    // Open the popin to edit the current pin
    $scope.openEditPopin = function () {
        const popin = ngDialog.open({
            template: 'common/vboardAddPinDialog/templates/vboardAddPinDialog.html',
            controller: 'VboardAddPinDialogController',
            className: 'ngdialog-theme-default ngdialog-theme-addPin',
            scope: $scope
        });
        popin.closePromise.then(function (data) {
            if (data && data.value && data.value==='OK') {
                $scope.info = "mise à jour";
                ngDialog.open({
                    template: 'common/vboardAddPinDialogOK/templates/vboardAddPinDialogOK.html',
                    scope: $scope
                });
            }
        });
    };


    /** Newsletter Action */

    // Allow a newsletter user to add or remove the tad #newsletter to a pin
    $scope.toggleNewsletterTag = function () {
        $http.post(`${ CONFIG.apiEndpoint  }/pins/toggleNewsletterLabel/${  $scope.pin.pinId }`).then(function () {
            vboardMessageInterceptor.showInfoMessage("Label changé");
        }, function (error) {
            console.error('error: ', error);
        });
    };


    /** SavedPin */

    // Save a pin to be able to read it later without searching too long for it. (Set as favorite)
    $scope.savePin = function () {
        $http.post(`${ CONFIG.apiEndpoint  }/savedpins/${  $scope.pin.pinId }`).then(function () {
            vboardMessageInterceptor.showInfoMessage("Epingle enregistrée");
            $scope.saved.push($scope.pin.pinId);
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'savePin');
        });
    };

    // UnSave a pin (opposite of previous function)
    $scope.unSavePin = function () {
        $http.delete(`${ CONFIG.apiEndpoint  }/savedpins/${  $scope.pin.pinId }`).then(function () {
            vboardMessageInterceptor.showInfoMessage("Epingle retirée des favoris");
            const index = $scope.saved.indexOf($scope.pin.pinId);
            if (index > -1) {
                $scope.saved.splice(index, 1);
            }
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'unSavePin');
        });
    };

    /** Events */
    $scope.$watch('pin.imgType', function () {
        $scope.loadImage();
    });

    $scope.$on('userAuthenticated', function () {
        $scope.setPerm();
    });

    $scope.$on('vboardUserLastConnectionSet', function () {
        if ($rootScope.lastConnection && (new Date($rootScope.lastConnection).getTime() - new Date($scope.pin.postDateUTC).getTime() <= 0)) {
            $scope.pinClass = "vboardPin--unsee";
        }
    });

    /** Init */
    $scope.setPerm();
    // Check whether the user format is valid (version compatibility)
    const fullAuthor = $scope.pin.author && $scope.pin.author.split(',').length === 3;
    if (fullAuthor) {
        const [authorFirstName, authorLastName, authorEmail] = $scope.pin.author.split(',');
        $scope.pinAvatar = `/avatar/${  authorEmail  }.png`;
        $scope.readableAuthor = `${ authorFirstName } ${ authorLastName }`;
    } else {
        vboardAuth.getUserByEmail($scope.pin.author).then(function (success) {
            if (success) {
                $scope.readableAuthor = `${ success.first_name  } ${  success.last_name }`;
                $scope.authorLink = `#profil/${  success.email }`;
                if (success.avatar) {
                    $scope.pinAvatar = `/avatar/${  success.email  }.png`;
                }
            } else {
                // Find or create the user if the name of the author is only constituted of an email (used mainly for vblog pins)
                if ($rootScope.userAuthenticated && $scope.pin.author === $rootScope.userAuthenticated.email) {
                    vboardAuth.getUser($rootScope.userAuthenticated);
                }
            }
        });
    }

    // Enable bootstrap tooltip using jquery
    $('[data-toggle="tooltip"]').tooltip();
});
