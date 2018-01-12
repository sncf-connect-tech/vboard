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
    blogUrl: '$VBOARD_WP_PUBLIC_HOST',
    localisations: [
        {id: 'Nantes1', name: 'Nantes 1er Etage'},
        {id: 'Nantes2', name: 'Nantes 2eme Etage'},
        {id: 'Nantes5', name: 'Nantes 5eme Etage'},
        {id: 'Nantes6', name: 'Nantes 6eme Etage'},
        {id: 'Collines2', name: 'Collines 2eme Etage'},
        {id: 'Collines3', name: 'Collines 3eme Etage'},
        {id: 'Collines8', name: 'Collines 8eme Etage'}
    ],
});
