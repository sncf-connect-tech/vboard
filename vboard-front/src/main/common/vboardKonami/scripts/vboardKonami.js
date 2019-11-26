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

angular.module('vboard').controller('VboardKonami', function ($scope, vboardMessageInterceptor, $http, CONFIG, $location) {

    var konami = [38, 38, 40, 40, 37, 39, 37, 39, 66, 65];
    var index = 0;
    var executed = false;
    var nbexecution = 0;

    var messageDisplay = function () {
        switch (nbexecution) {
        case 2:
            vboardMessageInterceptor.showSuccessMessage("Quoi ? Encore ? Non ! Du moins pas maintenant !");
            break;
        case 3:
            vboardMessageInterceptor.showSuccessMessage("Curieux et persistant en plus. Toujours pas.");
            break;
        case 4:
            vboardMessageInterceptor.showSuccessMessage("Le but c'est de voir quand je serais à court de phrases, c'est cela ?");
            break;
        case 5:
            vboardMessageInterceptor.showSuccessMessage("Bon, ok, c'est ma limite, rechargez la page, vous pourrez recommencer le Konami Code");
            break;
        case 6:
            vboardMessageInterceptor.showSuccessMessage("C'est pas vrai, vous ne l'avez même pas rechargé !");
            break;
        default:
            vboardMessageInterceptor.showSuccessMessage("Bon, ok, je réinitialise tout. La prochaine fois, le Konami Code sera accessible !");
            executed = false;
            nbexecution = 0;
            break;
        }
    };

    $scope.keydown = function(event) {
        if (index === konami.length-1) {
            index = 0;
            nbexecution++;
            if (!executed) {
                vboardMessageInterceptor.showSuccessMessage("Konami Code !! (En cours de création, retentez plus tard)");
                executed = true;
                $http.post(CONFIG.apiEndpoint + '/gamification/secret');
                $http.post(CONFIG.apiEndpoint + '/konami/new/' + 0);
                $location.path('konami-page');
            } else {
                messageDisplay();
            }
        } else {
            if (event.keyCode === konami[index]) {
                index++;
            } else {
                index = 0;
            }
        }

    };

});

angular.module('vboard').controller('VboardKonamiExecute', function ($http, CONFIG, $scope, $rootScope) {

    /** Cache la barre de recherche */
    $rootScope.hideSearchField();

    $http.get(CONFIG.apiEndpoint + '/konami').then(function(response) {
        $scope.curious = response.data;
    });

    $scope.getNiceName = function (name) {
        return name.split(',')[0] + ' ' + name.split(',')[1];
    };

    $scope.getNiceDate = function (date) {
        return moment(date).fromNow();
    };

});

