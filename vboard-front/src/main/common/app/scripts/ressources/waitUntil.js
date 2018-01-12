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

angular.module('vboard').service('waitUntil', ['$q', '$timeout', function ($q, $timeout) {
    this.elemCreated = function (selector, timeout) {
        timeout = timeout || 1000;
        var deferred = $q.defer(),
            selected = angular.element(document.querySelector(selector));
        if (selected.length > 1) {
            deferred.reject(new Error('waitUntil.elemCreated("' + selector + '") : '
                + 'Too many elements match: ' + selected));
        } else if (selected.length) {
            deferred.resolve(selected[0]);
        } else {
            document.arrive(selector, function () {
                deferred.resolve(this);
            });
        }
        $timeout(function () {
            deferred.reject(new Error('waitUntil.elemCreated("' + selector + '") : '
                + 'TimeOut after ' + timeout + 'ms'));
        }, timeout);
        return deferred.promise;
    };
    this.elemRemoved = function (selector, timeout) {
        timeout = timeout || 1000;
        if (_.contains(selector, '>') || _.contains(selector, ' ')) {
            throw new Error('Descendent and child selectors not allowed: ' + selector);
        }
        var deferred = $q.defer();
        if (!$(selector).length) {
            deferred.resolve();
        } else {
            document.leave(selector, function () {
                deferred.resolve();
            });
        }
        $timeout(function () {
            deferred.reject(new Error('waitUntil.elemRemoved("' + selector + '") :'
                + 'TimeOut after ' + timeout + 'ms'));
        }, timeout);
        return deferred.promise;
    };
    this.allElemsRemoved = function (selector, timeout) {
        var self = this;
        return $q.all(_.map($(selector), function (elem) {
            if (!elem.id) {
                throw new Error('allElemsRemoved require selected elements to have ids');
            }
            return self.elemRemoved('#' + elem.id, timeout);
        }));
    };
}]);