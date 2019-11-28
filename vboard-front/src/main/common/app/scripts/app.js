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

angular.module('vboard', ['ngDialog', 'nsPopover', 'ngSanitize', 'ngRoute', 'ngImgCrop', 'ngCookies', 'ui.select']);

angular.module('vboard').run(function ($rootScope, $http, $timeout, $window, $interval, vboardPinsCollection, CONFIG, $location, vboardMessageInterceptor, $cookieStore) {

    // Used to change the color of a pin in light blue to show the user the pins added from its last connection.
    /* eslint-disable camelcase */
    $rootScope.lastConnection = $cookieStore.get('lastConnection');

    // Display the admin message if there is one
    vboardMessageInterceptor.getGeneralMessage();

    // CORS fix
    $http.defaults.withCredentials = true;

    const second = 1000;
    const minute = 60 * second;
    // Auto refresh every 2 minutes to retrieve new pins (posted by other)
    $interval(function () {
        // Prevent refreshing pins if the user is not on the default page (vboardpinboard)
        if ($location.url().indexOf("profil") < 0 && $location.url().indexOf("leaderboard") < 0 && $location.url().indexOf("search") < 0 && $location.url().indexOf("konami") < 0) {
            vboardPinsCollection.forceUpdate();
        }
        vboardMessageInterceptor.getGeneralMessage();
    }, 2*minute);

    $timeout(function () {
        // Set last connection to set it even if the user didn't quit the app
        $cookieStore.put('lastConnection', new Date());
        $http.post(`${ CONFIG.apiEndpoint  }/users/setLastConnection`);
    }, 30*second);

    $rootScope.$on('$locationChangeSuccess', function () {
        // A $rootScope listener cannot be defined inside a service (i.e. vboardPinsCollection), it has to be registered in `app.run`
        vboardPinsCollection.update();
    });

    // Set the lastConnection cookie and DB value before quitting the app.
    $window.onbeforeunload = function () {
        if ($rootScope.userAuthenticated) {
            $cookieStore.put('lastConnection', new Date());
            $http.post(`${ CONFIG.apiEndpoint  }/users/setLastConnection`);
        }
    };

});


angular.module('vboard').config(['$routeProvider',
    // Url allowed and controller/template linked
    function ($routeProvider) {
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
                controller: 'VboardKonamiExecuteController'
            })
            .otherwise({
                redirectTo: '/'
            });
    }
]);

angular.module('vboard').factory('authInterceptor', function authInterceptor($q, vboardKeycloakAuth) {
    return {
        request(config) {
            const defer = $q.defer();
            if (vboardKeycloakAuth.token) {
                vboardKeycloakAuth.updateToken(5).success(function () {
                    config.headers.Authorization = `Bearer ${  vboardKeycloakAuth.token }`;
                    defer.resolve(config);
                }).error(function () {
                    defer.resolve(config);
                });
            } else {
                defer.resolve(config);
            }
            return defer.promise;
        }
    }
});

angular.module('vboard').config(function ($httpProvider) {
    $httpProvider.interceptors.push('authInterceptor');
});

/* eslint-disable no-undef */
angular.element(document).ready(function () {

    if (typeof $window.Keycloak === 'undefined' || $window.Keycloak === 'DISABLED') {
        const isKeycloakVoluntarilyDisabled = typeof $window.Keycloak !== 'undefined';
        // If keycloak is unavailable, so is the client adapter
        // Therefore, we mock the adapter so the ui can load, and show an error message
        angular.module('vboard').factory('vboardKeycloakAuth', function vboardKeycloakAuth() {
            return { token: null, authenticated: isKeycloakVoluntarilyDisabled, login() {}, logout() {} };
        });
        angular.module('vboard').run(function vboardRun(vboardAuth) {
            // Loads user profile:
            vboardAuth.login();
        });
        if (!isKeycloakVoluntarilyDisabled) {
            angular.module('vboard').run(function (vboardMessageInterceptor) {
                vboardMessageInterceptor.showErrorMessage('Le serveur Keycloak est indisponible, vous ne pourrez donc pas vous connecter!');
            });
        }
        angular.bootstrap(document, ['vboard']);

    } else {

        const keycloak = new Keycloak('compile/scripts/keycloak.json');
        angular.module('vboard').factory('vboardKeycloakAuth', function vboardKeycloakAuth() {
            return keycloak;
        });
        angular.module('vboard').run(function (vboardAuth) {
            // Loads user profile:
            vboardAuth.login();
        });
        const options = {
            onLoad: 'check-sso',
            checkLoginIframe: false
        };
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken !== null) {
            options.refreshToken = refreshToken;
        }
        keycloak.init(options).success(function () {
            localStorage.setItem('refreshToken', keycloak.refreshToken);
            angular.bootstrap(document, ['vboard']);
        }).error(function (error) {
            console.error(error);
            angular.bootstrap(document, ['vboard']);
        });

    }

});
