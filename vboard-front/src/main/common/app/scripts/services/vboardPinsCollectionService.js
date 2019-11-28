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


/**
 * Service de gestion des Pins
 */
/* eslint-disable no-invalid-this */
/* eslint-disable-next-line angular/no-service-method */
angular.module('vboard').service('vboardPinsCollection', function vboardPinsCollection($rootScope, $q, $location, $http, VboardPin, vboardPinsCollectionUtils, CONFIG, vboardMessageInterceptor) {

    /**
     * Private section
     */
    let getByPopularity = false;
    let isUseLookingAtFavoritePins = false;

    /* Get Elasticsearch Pins */
    const fetchPins = function (text, from, offset) {
        isUseLookingAtFavoritePins = false;
        const search = getByPopularity ? '/popular' : '';
        // API call
        return $http.get(`${ CONFIG.apiEndpoint  }/pins${  search }`, {
            params: {
                text,
                from,
                offset
            }
        }).then(function (response) {
            if (response.status !== 200) {
                throw new Error(`Pins search failed:${  JSON.stringify(response) }`);
            }
            // Format the pins (see vboardPinService.js)
            return _.map(response.data, function (datum) {
                return new VboardPin(datum);
            });
        });
    };

    /* Get Elasticsearch Pins posted by an author*/
    const fetchPinsByAuthor = function (author, from) {
        // API call
        return $http.get(`${ CONFIG.apiEndpoint  }/pins/author`, {
            params: {
                author,
                from
            }
        }).then(function (response) {
            if (response.status !== 200) {
                throw new Error(`Pins search failed:${  JSON.stringify(response) }`);
            }
            return _.map(response.data, function (datum) {
                return new VboardPin(datum);
            });
        });
    };

    /* Get Elasticsearch Pin with a certain id */
    const fetchPinById = function (id) {
        // API call
        return $http.get(`${ CONFIG.apiEndpoint  }/pins/id/${  id }`).then(function (response) {
            if (response.status !== 200) {
                throw new Error(`Pin search failed:${  JSON.stringify(response) }`);
            }
            // Format the pins (see vboardPinService.js)
            return _.map(response.data, function (datum) {
                return new VboardPin(datum);
            });
        });
    };

    /**
     * Public section
     */
    this.lastSearch = {}; // Parameters in the last Search
    this.allPins = []; // All returned pins
    this.pins = []; // Pin displayed (according to label selected
    this.labels = []; // All label displayed (from labels contained in allPins)

    // Value of the user scroll (0 for top of the page)
    let scrollFrom = 0;

    /** Get params from URL ($location) which are  ?text or/and ?label, ?id, ?from, offset */
    /* eslint-disable complexity */
    const getControlParams = function () {
        const urlParams = $location.search();
        return {
            text: urlParams.text || '',
            author: urlParams.author || '',
            label: urlParams.label || '',
            id: urlParams.id || '',
            from: urlParams.from || vboardPinsCollectionUtils.initialFrom(),
            offset: urlParams.offset || scrollFrom
        };
    };


    /** Entry point : pins & labels update according to some criteria */
    this.update = function () {
        const self = this;
        const ctrlParams = getControlParams();
        // Test if we should call ELS
        if (ctrlParams.author) {
            // The @ symbol is removed because by default elasticsearch tokenize elements. It is to prevent making a static mapping of the els instance
            fetchPinsByAuthor(ctrlParams.author.substring(0, ctrlParams.author.indexOf('@')), ctrlParams.from).then(function (fetchedPins) {
                self.allPins = fetchedPins;
                self.replacePinsAndLabels();
                $rootScope.$broadcast('vboardPinsCollectionUpdated');
            });
        } else if (ctrlParams.id && ctrlParams.id !== self.lastSearch.id) {
            fetchPinById(ctrlParams.id).then(function (fetchedPins) {
                self.allPins = fetchedPins;
                self.replacePinsAndLabels();
                $rootScope.$broadcast('vboardPinsCollectionUpdated');
            });
        } else {
            if (ctrlParams.text !== self.lastSearch.text || ctrlParams.from !== self.lastSearch.from) {
                // update last search
                self.lastSearch = ctrlParams;
                // Elasticsearch Call
                fetchPins(ctrlParams.text, ctrlParams.from, ctrlParams.offset).then(function (fetchedPins) {
                    if (scrollFrom === 0) {
                        self.allPins = fetchedPins;
                        self.replacePinsAndLabels();
                        $rootScope.$broadcast('vboardPinsCollectionUpdated');
                    }
                });
            } else if (ctrlParams.label !== self.lastSearch.label) {
                // update last search
                self.lastSearch = ctrlParams;
                // Filter all pins by label
                self.filterPinsByLabel();
                $rootScope.$broadcast('vboardPinsCollectionUpdated');
            }
        }
    };

    /** Force pins and labels update */
    this.forceUpdate = function () {
        const self = this;
        const ctrlParams = getControlParams();
        if (ctrlParams.author) {
            // The @ symbol is removed because by default elasticsearch tokenize elements. It is to prevent making a static mapping of the els instance
            fetchPinsByAuthor(ctrlParams.author.substring(0, ctrlParams.author.indexOf('@')), ctrlParams.from).then(function (fetchedPins) {
                self.allPins = fetchedPins;
                self.replacePinsAndLabels();
                $rootScope.$broadcast('vboardPinsCollectionUpdated');
            });
        } else {
            if (scrollFrom === 0 && !isUseLookingAtFavoritePins && !ctrlParams.id) {
                fetchPins(ctrlParams.text, ctrlParams.from, ctrlParams.offset).then(function (fetchedPins) {
                    self.allPins = fetchedPins;
                    self.replacePinsAndLabels();
                    $rootScope.$broadcast('vboardPinsCollectionUpdated');
                });
            }
            if (ctrlParams.id) {
                fetchPinById(ctrlParams.id).then(function (fetchedPins) {
                    self.allPins = fetchedPins;
                    self.replacePinsAndLabels();
                    $rootScope.$broadcast('vboardPinsCollectionUpdated');
                });
            }
        }
    };

    /** Return the mysql pins that the current user has saved to be displayed */
    this.getSavedPins = function () {
        isUseLookingAtFavoritePins = true;
        this.resetScroll();
        const self = this;
        // Get the savedPins object
        $http.get(`${ CONFIG.apiEndpoint  }/savedpins`).then(function (response) {
            if (response.status !== 200) {
                throw new Error(`Pins search failed:${  JSON.stringify(response) }`);
            }
            const listPin = [];
            let nb = response.data.length;
            response.data.forEach(function (savedPin) {
                // For each getSavedPins, get the actual pins to display
                $http.get(`${ CONFIG.apiEndpoint  }/pins/${  savedPin.pin_id }`).then(function (response2) {
                    // If a pin has been deleted (but not the saved one, which is only a precaution, it shouldn't happend
                    // The server will return an empty data.
                    if (_.isEmpty(response2.data)) {
                        nb--; // if the pin does not exist, there will be one less pin in the listPin
                    } else {
                        listPin.push(response2.data);
                    }
                    // When all pins have been included in the listPin, we call the displaying function
                    if (listPin.length === nb) {
                        self.displaySavedPins(listPin);
                    }
                });
            });

        });
    };

    this.displaySavedPins = function (listPin) {
        const self = this;
        // Pins are sorted by date
        listPin = _.sortBy(listPin, (pin) => -(new Date(pin.post_date_utc)));
        // And mapped as vboardPinService.js model
        self.allPins = _.map(listPin, function (datum) {
            return new VboardPin(datum);
        });
        self.replacePinsAndLabels();
        $rootScope.$broadcast('vboardPinsCollectionUpdated');
    };

    // When the user wants to stop looking at its savedPins
    this.cancelSavedPins = function () {
        isUseLookingAtFavoritePins = false;
        this.forceUpdate();
    };


    this.resetScroll = function () {
        scrollFrom = 0;
    };

    // Make a new update with most popular pins (in the last 3 years)
    this.setSearchByPopularity = function (searchByLikes) {
        getByPopularity = searchByLikes;
        isUseLookingAtFavoritePins = false;
        this.resetScroll();
        this.forceUpdate();
    };

    /** Get the number of pins posted by a user */
    this.getPinsNumber = function (authorEmail) {
        // The @ symbole is removed because by default elasticsearch tokenize elements. It is to prevent making a static mapping of the els instance
        return fetchPinsByAuthor(authorEmail.substring(0, authorEmail.indexOf('@')), '').then(function (fetchedPins) {
            return fetchedPins.length;
        });
    };

    /** Get more pins (the 50 ones following) */
    this.fetchMore = function () {
        const ctrlParams = getControlParams();
        ctrlParams.offset += 50;
        scrollFrom = ctrlParams.offset;
        const self = this;
        // Will only be activated if the user was just scrolling (parameters in url are the same and the user was not looking to its saved pins
        if ((ctrlParams.text !== self.lastSearch.text || ctrlParams.from !== self.lastSearch.from || ctrlParams.offset !== self.lastSearch.offset) && !isUseLookingAtFavoritePins) {
            // update last search
            self.lastSearch = ctrlParams;
            // ELS call
            fetchPins(ctrlParams.text, ctrlParams.from, ctrlParams.offset).then(function (fetchedPins) {
                if (fetchedPins.length !== 0) {
                    self.allPins = self.allPins.concat(fetchedPins);
                    self.replacePinsAndLabels();
                    $rootScope.$broadcast('vboardPinsCollectionUpdated');
                }
            });
        }
    };

    this.fetchNoMore = function () {
        scrollFrom = 0;
    };

    /** Add a new pin */
    this.addPin = function (pinTitle, pinUrl, pinImgTeam, pinImgType, pinDescription, pinLabels, pinAuthor) {
        return $http.post(`${ CONFIG.apiEndpoint  }/pins`, {
            title: pinTitle,
            url: pinUrl,
            imgType: pinImgType,
            description: pinDescription,
            labels: pinLabels,
            author: pinAuthor
        });
    };

    /** Pin update */
    this.updatePin = function (pinId, pinTitle, pinUrl, pinImgTeam, pinImgType, pinDescription, pinLabels, pinAuthor) {
        return $http.post(`${ CONFIG.apiEndpoint  }/pins/update/${  pinId }`, {
            title: pinTitle,
            url: pinUrl,
            imgType: pinImgType,
            description: pinDescription,
            labels: pinLabels,
            author: pinAuthor
        });
    };


    /** Pin deletion */
    this.deletePin = function (pinId) {
        return $http.delete(`${ CONFIG.apiEndpoint  }/pins/${ pinId }`).then(function () {
            // send an event to remove it from the user view
            $rootScope.$broadcast('vboardPinsCollectionUpdated');
        });
    };


    // Called in a view update
    this.replacePinsAndLabels = function () {
        if (_.isEmpty(this.allPins)) {
            this.labels = [];
            this.pins = []
            return;
        }
        // Replace pins
        this.filterPinsByLabel();
        // Replace labels
        const labelsImploded = _.pluck(this.pins, 'labels');
        const labelsExploded = _.chain(labelsImploded)
            .map(function (labels) {
                return labels.split(',');
            }).flatten().filter(function (label) {
                return label.indexOf('#') === 0;
            }).value();

        // To adapt in different screen size and prevent the label line to be displayed on two lines
        let labelNumber = 12;
        if ($(window).width() < 1600 ) {
            labelNumber = 7;
        }
        if ($(window).width() < 1000 ) {
            labelNumber = 4;
        }
        if ($(window).width() < 800 ) {
            labelNumber = 2;
        }
        // removed duplicate, and sort by most common used. Then limit the number as define above
        this.labels = _.chain(labelsExploded).countBy(_.identity).pairs().sortBy(1).reverse()
            .pluck(0)
            .take(labelNumber)
            .value();
    };

    /** Retrieve all labels on displayed pins */
    this.getLabels = function (pins) {
        const labelsImploded = _.pluck(pins, 'labels');
        // Get all labels on pins, separate them (on pins mutliple labels are represented in a single string with ",")
        // Add get the ones with #
        const labelsExploded = _.chain(labelsImploded)
            .map(function (labels) {
                return labels.split(',');
            }).flatten().filter(function (label) {
                return label.indexOf('#') === 0;
            }).value();
        // removed duplicate, and sort by most common used.
        return _.chain(labelsExploded).countBy(_.identity).pairs().sortBy(1).reverse()
            .pluck(0)
            .value();
    };

    this.getAllLabelsOnPins = function () {
        return this.getLabels(this.pins);
    };

    this.getAllLabels = function () {
        return this.getLabels(this.allPins);
    };

    // Get every labels once put on a pin (mysql search)
    this.getEveryLabels = function () {
        return $http.get(`${ CONFIG.apiEndpoint  }/labels`).then(function (response) {
            return response.data;
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'getEveryLabels');
        });
    };

    this.filterPinsByLabel = function () {
        const { label } = this.lastSearch;
        if (label) {
            const labels = label.split('#');
            // To remove the first empty entity (the first label stating with a #)
            labels.shift();
            this.pins = _.filter(this.allPins, function (pin) {
                for (let labelIndex = 0; labelIndex < labels.length; labelIndex++) {
                    if (pin.labels.indexOf(labels[labelIndex]) >= 0) {
                        return true;
                    }
                }
                return false;
            });
        } else {
            this.pins = this.allPins;
        }
    };

    this.sendBroadcastUpdate = function () {
        $rootScope.$broadcast('vboardPinsCollectionUpdated');
    };

    /* Retrieve savedPins by the user to be able to show if a pin has already been saved or not */
    this.getUserSavedPins = function () {
        // API call
        return $http.get(`${ CONFIG.apiEndpoint  }/savedpins`).then(function (response) {
            return response;
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'getUserSavedPins');
        });
    };

    /* Retrieve all likes for a user */
    this.getUserLikes = function () {
        // API call
        return $http.get(`${ CONFIG.apiEndpoint  }/likes/by_user/${ $rootScope.userAuthenticated.email }`).then(function (response) {
            return response;
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'getUserLikes');
        });
    };

    this.addLike = function (pin) {
        // API call
        return $http.post(`${ CONFIG.apiEndpoint  }/likes/`, {
            pinId: pin,
            email: $rootScope.userAuthenticated.email
        });
    };

    this.removeLike = function (pin) {
        // API call
        return $http.delete(`${ CONFIG.apiEndpoint  }/likes/delete`, {
            params: {
                pinId: pin,
                email: $rootScope.userAuthenticated.email
            }
        });
    };

    /* Show all likes (who) on a pin */
    this.seeLikes = function (pinId) {
        // API call
        return $http.get(`${ CONFIG.apiEndpoint  }/likes/by_pin/${  pinId }`).then(function (response) {
            return response;
        }, function errorCallBack(error) {
            vboardMessageInterceptor.showError(error, 'seeLikes');
        });
    };

    this.addComment = function (comment, pinId) {
        return $http.post(`${ CONFIG.apiEndpoint  }/comments/`, {
            text: comment,
            pinId,
            author: `${ $rootScope.userAuthenticated.first_name  },${  $rootScope.userAuthenticated.last_name  },${  $rootScope.userAuthenticated.email }`
        }).then(function (response) {
            return response;
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'addComment');
        });
    };

    this.updateComment = function (commentId, text) {
        return $http.post(`${ CONFIG.apiEndpoint  }/comments/update/${  commentId }`, {
            text
        }).then(function (response) {
            return response;
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'updateComment');
        });
    };

    /* Retrieve all comments on a pin */
    this.getComments = function (pinId) {
        return $http.get(`${ CONFIG.apiEndpoint  }/comments/${  pinId }`).then(function (response) {
            return response;
        }, function (error) {
            vboardMessageInterceptor.showError(error, 'getComments');
        });
    };

});

/**
 * Service for pin management (not really used, but can be used to scroll not by adding a certain amont of pins but the pins from the last 3 months each time)
 */
angular.module('vboard').factory('vboardPinsCollectionUtils', function vboardPinsCollectionUtils(CONFIG) {
    /* Return a date like '2015-04-01' which is now minus xxx months */
    return {
        initialFrom() {
            const initFrom = moment().subtract(CONFIG.displayPinsFromLastMonthsCount, 'months');
            return initFrom.format('YYYY-MM-DD');
        },
    };
});