/*
 * This file is part of the vboard distribution.
 * (https://github.com/sncf-connect-tech/vboard)
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


/**
 * Catch errors when API calls do not return a valid response (to put in a promise or call back).
 */
// Too much a of refactoring needed:
/* eslint-disable no-invalid-this */
/* eslint-disable-next-line angular/no-service-method */
angular.module('vboard').service('vboardMessageInterceptor', function vboardMessageInterceptor($rootScope, $timeout, $http, CONFIG) {

    let messageDisplay = false;
    const SHORT_TIME_DISPLAY = 4000;
    const AVERAGE_TIME_DISPLAY = 6000;
    const LONG_TIME_DISPLAY = 8000;

    function SuccessMessage(message) {
        this.content = message;
    }

    function ErrorMessage(message) {
        this.content = message;
    }

    function WarningMessage(message) {
        this.content = message;
    }

    function InfoMessage(message) {
        this.content = message;
    }

    this.showSuccessMessage = function (message) {
        this.showMessage(new SuccessMessage(message))
    };

    this.showWarningMessage = function (message) {
        this.showMessage(new WarningMessage(message))
    };

    this.showErrorMessage = function (message) {
        this.showMessage(new ErrorMessage(message))
    };

    this.showError = function (error, callContext) {
        // the call to parseInt handle the case of the zero string: '0'
        let message = parseInt(error.status, 10) ? `HTTP ${ error.status } ${ error.data && error.data.message || error.data }` : `${ error }`;
        if (callContext) {
            message += ` in ${  callContext }`;
        }
        this.showErrorMessage(message)
        console.error(error);
    };

    this.showInfoMessage = function (message) {
        this.showMessage(new InfoMessage(message))
    };

    this.showMessage = function (message) {
        $rootScope.message = { type: null, message: null };
        $rootScope.message.content = message.content;
        $rootScope.message.show = true;

        this.setMessageClass(message);

        this.hideMessage(this.getMessageDelay(message));

    };

    /* eslint-disable complexity */
    this.setMessageClass = function (message) {
        switch (message.constructor) {
        case SuccessMessage:
            $rootScope.message.class = "alert-success";
            $rootScope.message.type = "Succès";
            break;
        case InfoMessage:
            $rootScope.message.class = "alert-info";
            $rootScope.message.type = "Info";
            break;
        case WarningMessage:
            $rootScope.message.class = "alert-warning";
            $rootScope.message.type = "Warning";
            break;
        case ErrorMessage:
            $rootScope.message.class = "alert-danger";
            $rootScope.message.type = "Erreur";
            break;
        default:
            $rootScope.message.class = "alert-info";
            $rootScope.message.type = "";
        }

    };

    /** Each type of message appear a couple of secondes (longer for more critical messages) */
    /* eslint-disable complexity */
    this.getMessageDelay = function (message) {
        let delay = LONG_TIME_DISPLAY;
        switch (message.constructor) {
        case SuccessMessage:
        case InfoMessage:
            delay = SHORT_TIME_DISPLAY;
            break;
        case WarningMessage:
            delay = AVERAGE_TIME_DISPLAY;
            break;
        case ErrorMessage:
            delay = LONG_TIME_DISPLAY;
            break;
        default:
            delay = SHORT_TIME_DISPLAY;
        }

        return delay;
    };

    this.hideMessage = function (delay) {
        $timeout(function () {
            $rootScope.message.type = null;
        }, delay);
    };

    this.showMessageAdmin = function (type, message) {
        $http.post(`${ CONFIG.apiEndpoint  }/messages`, {
            type,
            content: message
        });
        this.showGeneralMessage(type, message);
    };

    this.showGeneralMessage = function (type, message) {
        if (!messageDisplay) {
            $rootScope.message = { type: null, message: null };
            $rootScope.message.type = type;
            $rootScope.message.content = message;
            $rootScope.message.show = true;

            if (type === "Info") {
                $rootScope.message.class = "alert-info";
            }
            if (type === "Warning") {
                $rootScope.message.class = "alert-warning";
            }
            if (type === "Error") {
                $rootScope.message.class = "alert-danger";
            }
            messageDisplay = true;
        }
    };

    this.hideMessageAdmin = function () {
        $http.post(`${ CONFIG.apiEndpoint  }/messages/remove`);
        $rootScope.message.type = null;
    };

    this.getGeneralMessage = function () {
        const self = this;
        $http.get(`${ CONFIG.apiEndpoint  }/messages`).then( function (success) {
            if (success.data === null || success.data === "") {
                messageDisplay = false;
            } else {
                self.showGeneralMessage(success.data.type, success.data.content);
            }
        });
    };

});
