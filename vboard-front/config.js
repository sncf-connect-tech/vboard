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

const apiEndpoint = '$VBOARD_API_ENDPOINT',
    blogUrl = '$VBOARD_BLOG_URL',
    supportUrl = '$VBOARD_SUPPORT_URL',
    localisations = '$VBOARD_LOCALISATIONS',
    displayPinsFromLastMonthsCount = '$VBOARD_PINS_MONTHS_COUNT';

const isDefined = (str) => str && !str.startsWith('$')
const localisationParser = (loc) => {
    const [id, name] = loc.split(':');
    return { id, name };
}

angular.module('vboard').constant('CONFIG', {
    apiEndpoint: isDefined(apiEndpoint)                                       ? apiEndpoint                                      : 'http://localhost:8080',
    blogUrl: isDefined(blogUrl)                                               ? blogUrl                                          : null,
    supportUrl: isDefined(supportUrl)                                         ? supportUrl                                       : null,
    // By default, display only pins from the last 3 years:
    localisations: isDefined(localisations)                                   ? localisations.split(';').map(localisationParser) : [],
    displayPinsFromLastMonthsCount: isDefined(displayPinsFromLastMonthsCount) ? displayPinsFromLastMonthsCount                   : 36,
});
