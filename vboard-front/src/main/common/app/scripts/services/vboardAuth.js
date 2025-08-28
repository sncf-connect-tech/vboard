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


/**
 * Service to manage authentication
 */
angular.module('vboard').factory('vboardAuth', function vboardAuth($rootScope, $http, vboardMessageInterceptor, CONFIG, vboardPinsCollection, vboardKeycloakAuth) {
    return {
        login() {
            if (angular.isUndefined($rootScope.userAuthenticated) && vboardKeycloakAuth.authenticated) {
                $http.get(`${ CONFIG.apiEndpoint  }/authentication/login`).then(function (response) {
                    const user = response.data;
                    $rootScope.userAuthenticated = user;
                    $rootScope.$broadcast('userAuthenticated');
                    $rootScope.userAuthenticated.labels = user.favorite_labels;
                    if (user.last_connection) {
                        $rootScope.userAuthenticated.lastConnection = user.last_connection;
                        $rootScope.$broadcast('vboardUserLastConnectionSet');
                    }
                    $http.post(`${ CONFIG.apiEndpoint  }/gamification/connexion`);
                    $rootScope.islogin = false;
                    $rootScope.$broadcast('islogin');
                }).catch(function (err) {
                    vboardMessageInterceptor.showMessage("Erreur", err);
                });
            }
        },

        logout() {
            $http.post(`${ CONFIG.apiEndpoint  }/authentication/logout`).then(function () {
                vboardKeycloakAuth.logout();
            });
        },

        isAdmin() {
            return $rootScope.userAuthenticated && $rootScope.userAuthenticated.admin;
        },

        isModerator() {
            return $rootScope.userAuthenticated ? $rootScope.userAuthenticated.roles.includes('Moderateur'): false;
        },

        /** Get User */
        getUserByEmail(email) {
            return $http.get(`${ CONFIG.apiEndpoint  }/users/${  email }`).then(function (response) {
                if (response.status !== 200) {
                    throw new Error(`User search failed:${  JSON.stringify(response) }`);
                }
                return response.data;
            });
        },

        /** Get User */
        // Useful if when the parameter is a session user with limited parameters and this method returns the full user
        getUser(user) {
            const self = this;
            return this.getUserByEmail(user.email).then( function (success) {
                if (success === null || success === "") {
                    self.addUser(user);
                    return user;
                }

                return success;
            });
        },

        /** Add User */
        addUser(user) {
            return $http.post(`${ CONFIG.apiEndpoint  }/users`, {
                email: user.email,
                firstName: user.first_name,
                lastName: user.last_name
            });
        },

        /** Update User */
        updateUser(user) {
            return $http.post(`${ CONFIG.apiEndpoint  }/users/update`, {
                email: user.email,
                // To update:
                avatar: user.avatar,
                team: user.team,
                info: user.info,
                receiveNlEmails: user.receive_nl_emails,
                receivePopularPinsEmails: user.receive_popular_pins_emails,
                receiveLeaderboardEmails: user.receive_leaderboard_emails,
                receiveRecapEmails: user.receive_recap_emails
            });
        },

        /** Get the team object from its name */
        getTeam(name) {
            return $http.get(`${ CONFIG.apiEndpoint  }/teams/${  name }`).then(function (response) {
                if (response.status !== 200) {
                    throw new Error(`Team search failed:${  JSON.stringify(response) }`);
                }
                return response.data;
            }, function (error) {
                console.log('error: ', error );
            });
        },

        /** Add a new Team in the database */
        addTeam(name) {
            return $http.post(`${ CONFIG.apiEndpoint  }/teams`, {
                name
            });
        },

        /** Update a team */
        updateTeam(team) {
            return $http.post(`${ CONFIG.apiEndpoint  }/teams/update`, {
                name: team.name,
                email: team.email,
                avatar: team.avatar,
                info: team.info,
                latitude: team.latitude,
                longitude: team.longitude,
                localisation: team.localisation,
                project: team.project
            });
        },

        /** Set the list of users in a team (val is the list) */
        setMembers(name, val) {
            return $http.post(`${ CONFIG.apiEndpoint  }/teams/setMembers/${  name }`, {
                members: val.toString()
            });
        },

        /** Set a role to a user */
        setRole(email, role) {
            return $http.post(`${ CONFIG.apiEndpoint  }/users/setRole/${  role }`, {
                email
            });
        },

        /** Add a role to a user */
        addRole(email, role) {
            return $http.post(`${ CONFIG.apiEndpoint  }/users/addRole/${  role }`, {
                email
            });
        },

        /** Remove a role to a user */
        removeRole(email, role) {
            return $http.post(`${ CONFIG.apiEndpoint  }/users/removeRole/${  role }`, {
                email
            });
        },

        /** Get all user that have a specific role */
        getRole(role) {
            return $http.get(`${ CONFIG.apiEndpoint  }/users/getRole/${  role }`);
        },
    };
});
