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

/**
 * Service to manage authentication
 */
angular.module('vboard').service('vboardAuth', function($rootScope, $http, vboardMessageInterceptor, CONFIG, vboardPinsCollection, vboardKeycloakAuth) {

    this.login = function() {
        if (angular.isUndefined($rootScope.userAuthenticated) && vboardKeycloakAuth.authenticated) {
            $http.get(CONFIG.apiEndpoint + '/authentication/login').then(function(response) {
                var user = response.data;
                $rootScope.userAuthenticated = user;
                $rootScope.$broadcast('userAuthenticated');
                $rootScope.userAuthenticated.labels = user.favorite_labels;
                if (user.last_connection) {
                    $rootScope.userAuthenticated.lastConnection = user.last_connection;
                    $rootScope.$broadcast('vboardUserLastConnectionSet');
                }
                $http.post(CONFIG.apiEndpoint + '/gamification/connexion');
                $rootScope.islogin = false;
                $rootScope.$broadcast('islogin');
            }).catch(function(err) {
                vboardMessageInterceptor.showMessage("Erreur", err);
            });
        }
    };

    this.logout = function() {
        $http.post(CONFIG.apiEndpoint + '/authentication/logout').then(function() {
            vboardKeycloakAuth.logout();
        });
    };

    this.isAdmin = function () {
        return $rootScope.userAuthenticated ? $rootScope.userAuthenticated.isAdmin >= 0: false;
    };

    this.isModerator = function () {
        return $rootScope.userAuthenticated ? $rootScope.userAuthenticated.role.indexOf('Moderateur') >= 0: false;
    };

    /** Get User */
    this.getUserByEmail = function (email) {
        return $http.get(CONFIG.apiEndpoint + '/users/' + email).then(function (response) {
            if (response.status !== 200 || response.statusText !== "OK") {
                throw new Error('User search failed:' + JSON.stringify(response));
            }
            return response.data;
        }, function(error) {
            console.log('error: ', error );
        });
    };

    /** Get User */
    // Useful if when the parameter is a session user with limited parameters and this method returns the full user
    this.getUser = function (user) {
        var self = this;
        return this.getUserByEmail(user.email).then( function (success) {
            if (success === null || success === "") {
                self.addUser(user);
                return user;
            }

            return success;
        });
    };

    /** Add User */
    this.addUser = function (user) {
        return $http.post(CONFIG.apiEndpoint + '/users', {
            email: user.email,
            firstName: user.first_name,
            lastName: user.last_name
        });
    };

    /** Update User */
    this.updateUser = function (user) {
        return $http.post(CONFIG.apiEndpoint + '/users/update', {
            email: user.email, // As ID
            // To update:
            avatar: user.avatar,
            team: user.team,
            info: user.info,
            receiveNlEmails: user.receive_nl_emails,
            receivePopularPinsEmails: user.receive_popular_pins_emails,
            receiveLeaderboardEmails: user.receive_leaderboard_emails,
            receiveRecapEmails: user.receive_recap_emails
        });
    };

    /** Get the team object from its name */
    this.getTeam = function (name) {
        return $http.get(CONFIG.apiEndpoint + '/teams/' + name).then(function (response) {
            if (response.status !== 200 || response.statusText !== "OK") {
                throw new Error('Team search failed:' + JSON.stringify(response));
            }
            return response.data;
        }, function(error) {
            console.log('error: ', error );
        });
    };

    /** Add a new Team in the database */
    this.addTeam = function (name) {
        return $http.post(CONFIG.apiEndpoint + '/teams', {
            name: name
        });
    };

    /** Update a team */
    this.updateTeam = function (team) {
        return $http.post(CONFIG.apiEndpoint + '/teams/update', {
            name: team.name,
            email: team.email,
            avatar: team.avatar,
            info: team.info,
            latitude: team.latitude,
            longitude: team.longitude,
            localisation: team.localisation,
            project: team.project
        });
    };

    /** Set the list of users in a team (val is the list) */
    this.setMembers = function (name, val) {
        return $http.post(CONFIG.apiEndpoint + '/teams/setMembers/' + name, {
            members: val.toString()
        });
    };

    /** Set a role to a user */
    this.setRole = function (email, role) {
        return $http.post(CONFIG.apiEndpoint + '/users/setRole/' + role, {
            email: email
        });
    };

    /** Add a role to a user */
    this.addRole = function (email, role) {
        return $http.post(CONFIG.apiEndpoint + '/users/addRole/' + role, {
            email: email
        });
    };

    /** Remove a role to a user */
    this.removeRole = function (email, role) {
        return $http.post(CONFIG.apiEndpoint + '/users/removeRole/' + role, {
            email: email
        });
    };

    /** Get all user that have a specific role */
    this.getRole = function (role) {
        return $http.get(CONFIG.apiEndpoint + '/users/getRole/' + role);
    };

});
