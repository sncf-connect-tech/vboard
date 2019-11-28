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


angular.module('vboard').directive('vboardComment', function vboardComment() {
    return {
        restrict: 'E',
        scope: true, // new child scope
        templateUrl: 'common/vboardComment/templates/vboardComment.html',
        controller: 'VboardCommentController'
    }
});

angular.module('vboard').controller('VboardCommentController', function VboardCommentController($scope, $rootScope, $http, vboardAuth, vboardImgs, vboardMessageInterceptor, vboardPinsCollection, CONFIG, ngDialog) {

    $scope.avatar = "images/avatar.png";

    // If the scope give a pin and not a comment, the last comment from the pin is retrieved
    if (!$scope.comment) {
        $scope.comment = $scope.pin.lastComment
    }

    /** Get comment's author nice name */
    // Check if the author string has the right format (firstName,LastName,email)
    if ($scope.comment.author && $scope.comment.author.indexOf(',', $scope.comment.author.indexOf(',') + 1) > -1) {
        const [firstName, lastName, email] = $scope.comment.author.split(',');
        $scope.name = `${ firstName } ${ lastName }`;
        $scope.email = email;
        $scope.avatar = `/avatar/${  $scope.email  }.png`;
    } else {
        $scope.name = $scope.comment.author;
        $scope.email = $scope.comment.author;
    }

    /** Display Break lines **/
    $scope.comment.text = $scope.comment.text.replace(/\n/g, "<br />");

    /** pin's date of creation */
    $scope.date = moment($scope.comment.post_date_utc).fromNow();

    // Check if the user can modify/delete the comments, ie if he is the author or an admin
    $scope.perm = $rootScope.userAuthenticated ? ((`${ $rootScope.userAuthenticated.first_name  },${  $rootScope.userAuthenticated.last_name  },${  $rootScope.userAuthenticated.email }`) === $scope.comment.author ||
    vboardAuth.isAdmin() || vboardAuth.isModerator()): false;

    /** Comment functions */

    $scope.deleteComment = function () {
        const popin = ngDialog.open({
            template: 'common/vboardDeleteConfirm/templates/vboardDeleteCommentConfirm.html',
            controller: 'VboardDeleteCommentDialogController',
            scope: $scope
        });
        popin.closePromise.then(function () {
            $scope.comment = null;
            $scope.$parent.pin.commentsNumber--; // Reduce the number of comments written on the pin
            if ($scope.$parent.pin.commentsNumber === 1) {
                $scope.$parent.showComments();
            }
        });
    };

    $scope.modifyComment = function () {
        $scope.editComment = true;
        $scope.comment.text = $scope.comment.text.replace(/<br \/>/g, "\n"); // Written with \n html display with <br>
        vboardPinsCollection.sendBroadcastUpdate();

    };

    $scope.submitUpdatedComment = function () {
        vboardPinsCollection.updateComment($scope.comment.id, $scope.comment.text).then(function () {
            vboardMessageInterceptor.showInfoMessage("La modification du commentaire a été effectuée.");
            vboardPinsCollection.sendBroadcastUpdate();
        });
        $scope.editComment = false;
        $scope.comment.text = $scope.comment.text.replace(/\n/g, "<br />");
        vboardPinsCollection.sendBroadcastUpdate();
    };

});
