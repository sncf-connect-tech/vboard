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

angular.module('vboard').directive('vboardToolbar', function () {
    return {
        restrict: 'E',
        scope: {}, // Isolate scope
        templateUrl: 'common/vboardToolbar/templates/vboardToolbar.html',
        controller: 'VboardToolbarController',
        link: function ($scope, $element) {
            $element.bind('keydown', function (event) {
                if (event.keyCode === 13) {
                    $scope.search.label = '';
                    $scope.triggerSearch();
                }
            });
            $scope.syncSearchInputFromUrlParams();
        }
    }
});

angular.module('vboard').controller('VboardToolbarController', function ($scope, $rootScope, $window, ngDialog, $location, $http, $timeout, $interval, vboardPinsCollection, vboardMessageInterceptor, CONFIG, vboardAuth, vboardKeycloakAuth) {

    $scope.blogUrl = CONFIG.blogUrl;
    $scope.search = {
        text: '',
        label: ''
    };

    // Send the actual userAuthenticated scope to the view and get the user's notification
    $rootScope.$watch('userAuthenticated', function(newVal) {
        $scope.userAuthenticated = newVal;
        if ($rootScope.userAuthenticated) {
            $scope.getNotif();
        }
    });

    // Get if the authentication process is running or not
    $rootScope.$on('islogin', function() {
        $scope.islogin = $rootScope.islogin;
    });

    /** Hide the searchbar */
    $rootScope.hideSearchField = function () {
        $scope.showSearchInput = false;
    };

    /** Show the SearchBar */
    $rootScope.showSearchField = function () {
        $scope.showSearchInput = true;
    };

    // Launch the search by changing the url
    $scope.triggerSearch = function () {
        // triggers a $locationChangeSuccess
        $location.search({'text': $scope.search.text || null, 'label': $scope.search.label|| null});
    };

    // Force the refresh
    $scope.forceUpdate = function () {
        vboardPinsCollection.resetScroll();
        vboardPinsCollection.forceUpdate();
    };

    // Open a popin to add a new pin
    $scope.openAddPinDialog = function () {
        var popin = ngDialog.open({
            template: 'common/vboardAddPinDialog/templates/vboardAddPinDialog.html',
            controller: 'VboardAddPinDialogController',
            className: 'ngdialog-theme-default ngdialog-theme-addPin'
        });
        popin.closePromise.then(function(data) {
            if (data && data.value && data.value==='OK') {
                $scope.info = "ajoutée";
                ngDialog.open({
                    template: 'common/vboardAddPinDialogOK/templates/vboardAddPinDialogOK.html',
                    scope: $scope
                });
            }
        });
    };

    $scope.login = function () {
        vboardKeycloakAuth.login();
    };

    $scope.logout = function () {
        vboardAuth.logout();
    };

    // Synchronize filters with query parameters
    $scope.syncSearchInputFromUrlParams = function () {
        $scope.search.text = $location.search().text;
        $scope.search.label = $location.search().label;
    };

    // Boolean to know if we search for all labels on displayed pins or just the most used ones.
    $scope.getAllLabels = false;

    // Search the labels displayed on the pins
    $scope.getLabels = function () {
        var labels;
        if ($scope.getAllLabels) {
            labels = vboardPinsCollection.getAllLabelsOnPins();
        } else {
            labels = vboardPinsCollection.labels;
        }
        return labels;
    };

    // Show/hide all labels
    $scope.showAllLabels = function() {
        $scope.getAllLabels = !$scope.getAllLabels;
    };

    // Add or remove some label on filter
    $scope.addOrDeleteLabel = function (label, trigger) {
        if ($scope.search.label && $scope.search.label.split('#').indexOf(label.substring(1)) > -1) {
            // Update the search using an array. The symbol '#' is removed to check if the label is included in that array
            var index = $scope.search.label.split('#').indexOf(label.substring(1));
            var labels = $scope.search.label.split('#');
            labels.splice(index, 1);
            $scope.search.label = labels.join('#');
        } else {
            $scope.search.label = $scope.search.label ? $scope.search.label + label: label;
        }
        if (trigger) {
            $scope.triggerSearch();
        }
    };

    // Check whether the label is active, ie whether the label has been clicked on one time
    $scope.isActive = function(label) {
        var active = false;
        if ($scope.search.label) {
            active = $scope.search.label.split('#').indexOf(label.substring(1)) > -1;
        }
        return active;
    };

    // Synchronize filters with the URL
    $scope.$on('$locationChangeSuccess', function () {
        $scope.syncSearchInputFromUrlParams();
        $scope.userOnProfil = $location.url() === "/profil";

    });


    /* Check if the window is being focus or not */
    var windowFocus = true;

    $(window).focus(function() {
        windowFocus = true;
    }).blur(function() {
        windowFocus = false;
    });

    // Get the user's notification
    $scope.getNotif = function () {
        var previewNotificationNumber = $scope.notificationsUnseen ? $scope.notificationsUnseen.length : 0;
        // API call on the unclicked notification: notification on which the user never clicked on.
        $http.get(CONFIG.apiEndpoint + '/notifications/unclicked').then(function (response) {
            if (response.status !== 200) {
                throw new Error('Notifications get failed:' + JSON.stringify(response));
            }
            // Sort them by date
            $scope.notifications = _.sortBy(response.data, function(notif) { var date = new Date(notif.date); return -date });
            // Get all notification never seen by the user
            $scope.notificationsUnseen = $scope.notifications.filter(function (notif) {
                return !notif.seen;
            });
            // Get the notifications seen but not clicked by the user
            var notificationsSeen = $scope.notifications.filter(function (notif) {
                return notif.seen;
            });
            // If the notifications not seen are less than 5, the array is completed to get at least 5 notifications.
            if ($scope.notificationsUnseen.length < 5) {
                // Complete by seen but unclicked notifications
                $scope.notifications = $scope.notificationsUnseen.concat(notificationsSeen.slice(0, 5 - $scope.notificationsUnseen.length));
                $scope.notificationSeenLoaded = 5 - $scope.notificationsUnseen.length;
            }
            // Chrome and Firefox browser notification when the window is not focused on
            if ($scope.notificationsUnseen.length > 0 && !windowFocus && previewNotificationNumber !== $scope.notificationsUnseen.length) {
                var plural = $scope.notificationsUnseen.length === 1 ? '': 's';

                // Visually, the numbers of notifications over 99 are displayed as 99
                $scope.notification('VBoard', 'Vous avez ' + $scope.notificationsUnseen.length > 99 ? 99 : $scope.notificationsUnseen.length + ' nouvelle' + plural + ' notification' + plural);
            }
        }, function (error) {
            vboardMessageInterceptor.showErrorMessage("Vos notifications n'ont pas pu être chargées. (Status Code: " + error.status + ')');
            console.log('error: ', error);
        });
    };

    $scope.notificationSeenLoaded = 0;
    $scope.canLoadPreviousNotif = true;

    // See notification historic (add 5 notification by default: number)
    $scope.getSeenNotif = function (number) {
        $http.get(CONFIG.apiEndpoint + '/notifications/seen').then(function (response) {
            if (response.status !== 200) {
                throw new Error('Notifications get failed:' + JSON.stringify(response));
            }
            // Notification not seen are already all displayed.
            var notificationsSeen = _.sortBy(response.data, function(notif) { var date = new Date(notif.date); return -date });
            var notifNumber = $scope.notifications.length;
            // Add notifications not yet in the array
            $scope.notifications = $scope.notifications.concat(notificationsSeen.slice($scope.notificationSeenLoaded, number + $scope.notificationSeenLoaded));
            $scope.notificationSeenLoaded = number + $scope.notificationSeenLoaded;
            // If there is no more notification to load, canLoadPreviousNotif will hide the load more notifications button
            if (notifNumber === $scope.notifications.length) {
                $scope.canLoadPreviousNotif = false;
            }
        }, function (error) {
            vboardMessageInterceptor.showErrorMessage("Vos anciennes notifications n'ont pas pu être chargées. (Status Code: " + error.status + ')');
            console.log('error: ', error);
        });
    };

    // When the user click on the notification button, he sees the notification and so all notifications not seen are set to seen under 7s
    $scope.notificationsCheck = function () {
        $scope.notificationsUnseen.forEach( function(notif) {
            $http.post(CONFIG.apiEndpoint + '/notifications/seen/' + notif.id);
            $timeout(function () {
                _.remove($scope.notificationsUnseen, notif);
            }, 7000);
        });
    };

    var millisecond = 1;
    var second = millisecond * 1000;
    var minute = second * 60;
    // Auto refresh every 2 minutes to retrieve the last notifications
    $interval(function() {
        $scope.getNotif();
    }, minute*2);

    // Needed to allow browser notification (and ask the user for its authorisation)
    document.addEventListener('DOMContentLoaded', function () {
        if (Notification.permission !== "granted") {
            Notification.requestPermission();
        }
    });

    // Browser notification (Chrome and Firefox)
    $scope.notification = function (title, message) {
        if (Notification.permission !== "granted") {
            Notification.requestPermission();
        } else {
            var notification = new Notification(title, {
                icon: 'images/logo_vsc.jpg', // App logo
                body: message // message to display
            });
            // Action to do when the user click on the browser notification (here the window get focus)
            notification.onclick = function () {
                $window.focus();
            };
        }
    };

    // Get more pins (when scroll down)
    $scope.showMore = function () {
        vboardPinsCollection.fetchMore();
    };

    // Activate the search by most liked pins
    $scope.searchByPopularity = false;
    $scope.toggleSearchByPopularity = function () {
        $scope.displaySavedPins = false;
        $scope.searchByPopularity = !$scope.searchByPopularity;
        vboardPinsCollection.setSearchByPopularity($scope.searchByPopularity);
        $timeout(function () {
            vboardPinsCollection.forceUpdate();
        }, 5000);
    };

    // Simulate a click on all favorite labels (if the user has some)
    $scope.searchByFavoriteLabels = false;
    $scope.toggleSearchByFavoriteLabels = function () {
        var self = this;
        $scope.searchByFavoriteLabels = !$scope.searchByFavoriteLabels;
        if ($scope.searchByFavoriteLabels) {
            vboardAuth.getUser($rootScope.userAuthenticated).then(function (success) {
                var labels = success.favorite_labels.split(',');
                $scope.search.label = null;
                for (var i = 0; i < labels.length; i++) {
                    self.addOrDeleteLabel(labels[i], false);
                }
                $scope.triggerSearch();
            });
        } else {
            $scope.search.label = null;
            $scope.triggerSearch();
        }
    };

    // Display the user's saved pins
    $scope.displaySavedPins = false;
    $scope.toggleDisplaySavedPins = function () {
        $scope.displaySavedPins = !$scope.displaySavedPins;
        $scope.searchByPopularity = false;
        if ($scope.displaySavedPins) {
            vboardPinsCollection.getSavedPins();
        } else {
            vboardPinsCollection.cancelSavedPins();
        }
    }

});
