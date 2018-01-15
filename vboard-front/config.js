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

angular.module('vboard').constant('CONFIG', {
    apiEndpoint: '/api/v1',
    blogUrl: '$VBOARD_WP_PUBLIC_HOST',
    displayPinsFromLastMonthsCount: 36, // By default, only pins from the last 3 years can be seen (can be forced in the url call directly (?from=))
    localisations: '$VBOARD_LOCALISATIONS'.split(';').map(function (loc) { return {id: loc.split(':')[0], name: loc.split(':')[1]}; }),
});
