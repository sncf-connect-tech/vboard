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

angular.module('vboard').directive('vboardDeleteComment', function () {
    return {
        restrict: 'E',
        scope: true, // new child scope
        templateUrl: 'common/vboardDeleteConfirm/templates/vboardDeleteComment.html',
        controller: 'VboardDeleteCommentDialogController'
    }
});

angular.module('vboard').controller('VboardDeleteCommentDialogController', function (vboardPinsCollection, $scope, $http, CONFIG, vboardMessageInterceptor) {

    // Comment Deletion validation
    $scope.submit = function () {
        $http.delete(CONFIG.apiEndpoint + '/comments/', {
            params: {
                id: $scope.comment.id
            }
        }).then(function () {
            vboardMessageInterceptor.showInfoMessage("La suppression du commentaire a été effectuée.");
            $scope.closeThisDialog('OK'); // Close the popin
        }, function(error) {
            vboardMessageInterceptor.showErrorMessage("La suppression du commentaire a échoué. (Status Code: " + error.status + ')');
        });
    };

});
