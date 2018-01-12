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

angular.module('vboard').directive('vboardProfilNewsletter', function () {
    return {
        restrict: 'E',
        scope: {}, // Isolate scope
        templateUrl: 'common/vboardProfilRole/templates/vboardProfilNewsletter.html',
        controller: 'VboardProfilNewsletter'
    }
});

angular.module('vboard').controller('VboardProfilNewsletter', function ($scope, $rootScope, $http, API_ENDPOINT, ngDialog, vboardMessageInterceptor, vboardPinsCollection) {

    // Check if the last newsletter has been sent more than 20 days ago
    $scope.canSendNewsletter = false;

    // Used (in a link) to check which pins will appear in the newsletter
    $scope.dateLink = moment().subtract(1, 'month').format('YYYY-MM-DD');

    // Set the label used for the newsletter
    $scope.label = $rootScope.userAuthenticated.newsletter_label;

    $scope.setLabel = function (label) {
        $scope.label = label.indexOf('#') === 0 ? label : '#' + label;
        $http.post(API_ENDPOINT + '/users/nlLabel', {
            label: label
        });
        vboardMessageInterceptor.showSuccessMessage("Le label de newsletter a été sauvegardé");
    };

    vboardPinsCollection.getEveryLabels().then(function (allLabelsObject) {
        var allLabels = [];
        for (var labelValue in allLabelsObject) {
            allLabels.push((allLabelsObject[labelValue].label_name));
        }
        allLabels = allLabels.sort();
        $scope.labelSuggest = allLabels;
    });

    // Send an test email to the current user
    $scope.sendNewsletterTest = function (email) {
        if (email && email.title && email.message) {
            // Allow the user to write a multiline message in the body of the email (replace line break with <br>)
            var messageHTML = email.message.replace(/(?:\r\n|\r|\n)/g, '<br>');
            $http.post(API_ENDPOINT + '/messages/sendEmails/nltest', {
                title: email.title,
                firstMessage: messageHTML
            });
            vboardMessageInterceptor.showSuccessMessage("L'email vous a bien été envoyé (si non vide)");
        } else {
            vboardMessageInterceptor.showErrorMessage("Veuillez remplir les champs Titre et Message");
        }
    };

    // Newsletter submit confirmation
    $scope.displayNewsletterPopin = function (email) {
        if (email && email.title && email.message) {
            ngDialog.open({
                template: 'common/vboardProfilRole/templates/vboardNewsletterConfirm.html',
                scope: $scope
            });
        } else {
            vboardMessageInterceptor.showErrorMessage("Veuillez remplir les champs Titre et Message");
        }
    };

    // Submit the newsletter to all user (that accepts emails)
    $scope.newsletterSubmit = function (email) {
        var messageHTML = email.message.replace(/(?:\r\n|\r|\n)/g, '<br>');
        $http.post(API_ENDPOINT + '/messages/sendEmails/nl', {
            title: email.title,
            firstMessage: messageHTML
        });
        $scope.canSendNL = false;
        $scope.lastDate = moment().format('L');
        ngDialog.close();
    };

    // Retrieve the last newsletter object (which contain the date where it was sent)
    $http.get(API_ENDPOINT + '/messages/getLastNL').then(function (response) {
        $scope.lastDate = moment(response.data.post_date_utc).format('L');
        // Check whether the last newsletter was sent in the last 10 minutes (to avoid duplications)
        $scope.canSendNewsletter =  moment(response.data.post_date_utc).add(10, 'minutes').isBefore(moment());
    }, function (error) {
        console.log('error: ', error);
    });

});
