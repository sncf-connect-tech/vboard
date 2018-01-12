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
 * Service to manage Images
 */
angular.module('vboard').service('vboardImgs', function ($http, $q, $rootScope, API_ENDPOINT) {


    /** Retrieve user's avatar (base64) */
    this.getAvatar = function (email) {
        return $http.get(API_ENDPOINT + '/users/avatar/' + email).then(function (response) {
            if (response.status !== 200) {
                throw new Error('Avatar search failed:' + JSON.stringify(response));
            }
            return "data:image/png;base64," + response.data;
        }, function(error) {
            console.log('error: ', error );
        });
    };

    /** Retrieve pin's image */
    this.getPinImage = function (pinId) {
        return $http.get(API_ENDPOINT + '/pins/image/' + pinId).then(function (response) {
            if (response.status !== 200) {
                throw new Error('Pin Image search failed:' + JSON.stringify(response));
            }
            return response.data;
        }, function(error) {
            console.log('error: ', error );
        });
    };


});