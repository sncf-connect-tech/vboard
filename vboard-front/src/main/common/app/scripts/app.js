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

angular.module('vboard', ['ngDialog', 'nsPopover', 'ngSanitize', 'ngRoute', 'ngImgCrop', 'ngCookies', 'ui.select']);

angular.module('vboard').run(function ($rootScope, $http, $timeout, $window, $interval, vboardPinsCollection, CONFIG, $location, vboardMessageInterceptor, $cookieStore) {

    // Used to change the color of a pin in light blue to show the user the pins added from its last connection.
    /* eslint-disable camelcase */
    $rootScope.lastConnection = $cookieStore.get('lastConnection');

    // Display the admin message if there is one
    vboardMessageInterceptor.getGeneralMessage();

    // CORS fix
    $http.defaults.withCredentials = true;

    var millisecond = 1;
    var second = millisecond * 1000;
    var minute = second * 60;
    var hours = minute * 60;
    var needRefresh = false;
    // Auto refresh every 2 minutes to retrieve new pins (posted by other)
    $interval(function() {
        // Prevent refreshing pins if the user is not on the default page (vboardpinboard)
        if ($location.url().indexOf("profil") === -1 && $location.url().indexOf("leaderboard") === -1 && $location.url().indexOf("search") === -1 && $location.url().indexOf("konami") === -1) {
            vboardPinsCollection.forceUpdate();
        }
        vboardMessageInterceptor.getGeneralMessage();
        if (needRefresh) {
            vboardMessageInterceptor.showWarningMessage("Veuillez recharger votre page pour éviter l'expiration de votre token au niveau du serveur");
        }
    }, minute*2);

    // Warn the user to refresh the page prevent token expiration (causing error 500 (front still logged, but not back))
    $interval(function() {
        needRefresh = true;
    }, hours*20);

    // Display with packery can be sometimes tricky. Some display problems can occur at the start. Two force refresh are operated.
    $timeout(function () {
        vboardPinsCollection.forceUpdate();
    }, second);
    $timeout(function () {
        vboardPinsCollection.forceUpdate();
    }, second*10);

    $timeout(function () {
        // Set last connection to set it even if the user didn't quit the app
        $cookieStore.put('lastConnection', new Date());
        $http.post(CONFIG.apiEndpoint + '/users/setLastConnection');
    }, second*30);


    $rootScope.$on('$locationChangeSuccess', function () {
        // A $rootScope listener cannot be defined inside a service (i.e. vboardPinsCollection), it has to be registered in `app.run`
        vboardPinsCollection.update();
    });

    // Set the lastConnection cookie and DB value before quitting the app.
    window.onbeforeunload = function () {
        if ($rootScope.userAuthenticated) {
            $cookieStore.put('lastConnection', new Date());
            $http.post(CONFIG.apiEndpoint + '/users/setLastConnection');
        }
    };

});


angular.module('vboard').config(['$routeProvider',
    // Url allowed and controller/template linked
    function($routeProvider) {
        $routeProvider
            .when('/', {
                templateUrl: 'common/vboardContainer/templates/vboardContainer.html'
            })
            .when('/profil', {
                templateUrl: 'common/vboardProfilContainer/templates/vboardProfilContainer.html'
            })
            .when('/profil/team/:teamName', {
                templateUrl: 'common/vboardProfil/templates/vboardTeamProfil.html',
                controller: 'VboardTeamProfilController'
            })
            .when('/profil/:email', {
                templateUrl: 'common/vboardProfil/templates/vboardProfil.html',
                controller: 'VboardProfilPublicController'
            })
            .when('/leaderboard', {
                templateUrl: 'common/vboardLeaderBoard/templates/vboardLeaderBoardContainer.html'
            })
            .when('/search', {
                templateUrl: 'common/vboardProfilSearch/templates/vboardProfilSearch.html',
                controller: 'VboardProfilSearchController'
            })
            .when('/konami-page', {
                templateUrl: 'common/vboardKonami/templates/vboardKonami.html',
                controller: 'VboardKonamiExecute'
            })
            .otherwise({
                redirectTo: '/'
            });
    }
]);

angular.module('vboard').factory('authInterceptor', function($q, vboardKeycloakAuth) {
    return {
        request: function(config) {
            var defer = $q.defer();
            if (vboardKeycloakAuth.token) {
                vboardKeycloakAuth.updateToken(5).success(function() {
                    config.headers.Authorization = 'Bearer ' + vboardKeycloakAuth.token;
                    defer.resolve(config);
                }).error(function() {
                    defer.resolve(config);
                });
            } else {
                defer.resolve(config);
            }
            return defer.promise;
        }
    }
});

angular.module('vboard').config(function($httpProvider) {
    $httpProvider.interceptors.push('authInterceptor');
});

/* eslint-disable no-undef */
angular.element(document).ready(function() {

    if (typeof window.Keycloak === 'undefined' || window.Keycloak === 'DISABLED') {
        var isKeycloakVoluntarilyDisabled = typeof window.Keycloak !== 'undefined';
        // If keycloak is unavailable, so is the client adapter
        // Therefore, we mock the adapter so the ui can load, and show an error message
        angular.module('vboard').factory('vboardKeycloakAuth', function() {
            return { token: null, authenticated: isKeycloakVoluntarilyDisabled, login: function() {}, logout: function() {} };
        });
        angular.module('vboard').run(function(vboardAuth) {
            vboardAuth.login(); // Loads user profile
        });
        if (!isKeycloakVoluntarilyDisabled) {
            angular.module('vboard').run(function(vboardMessageInterceptor) {
                vboardMessageInterceptor.showErrorMessage('Le serveur Keycloak est indisponible, vous ne pourrez donc pas vous connecter!');
            });
        }
        angular.bootstrap(document, ['vboard']);

    } else {

        var keycloak = new Keycloak('compile/scripts/keycloak.json');
        angular.module('vboard').factory('vboardKeycloakAuth', function() {
            return keycloak;
        });
        angular.module('vboard').run(function(vboardAuth) {
            vboardAuth.login(); // Loads user profile
        });
        var options = {
            onLoad: 'check-sso',
            checkLoginIframe: false
        };
        var refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken !== null) {
            options.refreshToken = refreshToken;
        }
        keycloak.init(options).success(function() {
            localStorage.setItem('refreshToken', keycloak.refreshToken);
            angular.bootstrap(document, ['vboard']);
        }).error(function(error) {
            console.error(error);
            angular.bootstrap(document, ['vboard']);
        });

    }

});
