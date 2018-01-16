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

var apiEndpoint = '$VBOARD_API_ENDPOINT',
    blogUrl = '$VBOARD_WP_PUBLIC_HOST',
    localisations = '$VBOARD_LOCALISATIONS',
    displayPinsFromLastMonthsCount = '$VBOARD_PINS_MONTHS_COUNT';

var startsWithDollar = function (str) { return str.indexOf('$') === 0; };

angular.module('vboard').constant('CONFIG', {
    apiEndpoint: startsWithDollar(apiEndpoint) ? '/vboard' : apiEndpoint,
    blogUrl: startsWithDollar(blogUrl) ? null : blogUrl,
    // By default, display only pins from the last 3 years:
    localisations: startsWithDollar(localisations) ? [] : localisations.split(';').map(function (loc) { return {id: loc.split(':')[0], name: loc.split(':')[1]}; }),
    displayPinsFromLastMonthsCount: startsWithDollar(displayPinsFromLastMonthsCount) ? 36 : displayPinsFromLastMonthsCount,
});
