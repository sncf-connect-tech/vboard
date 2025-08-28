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


/* eslint-disable complexity */
angular.module('vboard').controller('VboardAddPinDialogController', function VboardAddPinDialogController(vboardPinsCollection, $scope, $rootScope, ngDialog, vboardImgs, vboardAuth, $http, CONFIG, $timeout, $sce, vboardMessageInterceptor) {

    // Init
    // $scope.pin is only defined when the pin is updated (and not created)
    $scope.displayLabels = '';
    $scope.error = false;
    $scope.popupTitle = 'Nouvelle épingle';
    $scope.videoLink = '';
    $scope.editablePin = {
        author: $rootScope.userAuthenticated ? `${ $rootScope.userAuthenticated.first_name  } ${  $rootScope.userAuthenticated.last_name }` : 'unknown',
        avatar: `/avatar/${  $rootScope.userAuthenticated.email  }.png`,
        date: moment().fromNow(),
        description: '',
        descriptionShow: '',
        labels: [],
        newLabel: '',
        title: '',
        url: ''
    };
    $scope.image = {
        croppedImage: '',
        originalImage: '',
        type: ''
    };


    /** Label */
    $scope.addLabel = function () {
        let label = $scope.editablePin.newLabel;
        // If the label does not start with #, one is added
        label = (label.startsWith('#') ? '' : '#') + label;
        $scope.editablePin.labels.push(label);
        $scope.displayLabels = $scope.editablePin.labels ? $scope.editablePin.labels.join(' ')  : '';
        $scope.editablePin.newLabel = '';
    };

    $scope.deleteLabel = function (ind) {
        $scope.editablePin.labels.splice(ind, 1);
        $scope.displayLabels = $scope.editablePin.labels ? $scope.editablePin.labels.join(' ')  : '';
    };

    // Add automatic tags (based on most popular pin labels put, if put regularly
    // If more than 50% of pins posted by a user have the same tags, they are automatically added in the creation of the pin (they can be removed)
    $http.get(`${ CONFIG.apiEndpoint  }/pins/getMostUsedLabels`).then(function (response) {
        if (!$scope.pin && response.data.length > 0) {
            const labels = response.data.split(",");
            labels.forEach(function (label) {
                $scope.editablePin.newLabel = label;
                $scope.addLabel();
            });
        }
    });

    // Used for autocompletion/suggestion
    vboardPinsCollection.getEveryLabels().then(function (allLabelsObject) {
        let allLabels = [];
        for (const labelValue in allLabelsObject) {
            allLabels.push((allLabelsObject[labelValue].label_name).slice(1));
        }
        allLabels = allLabels.sort();
        $scope.labelSuggests = allLabels;
    });


    /** Pin form */

    // validate the form submission (for adding a pin)
    $scope.submit = function () {
        // If the image is visible and start with http, it is send as it is
        if ($scope.image.show && $scope.image.croppedImage.startsWith("http")) {
            $scope.image.type = $scope.image.croppedImage;
            if ($scope.image.type.length >= 255) {
                vboardMessageInterceptor.showErrorMessage("L'url de l'image est trop longue (255 caractères maximum)");
            }
        } else {
            // If the image is only visible, what is before the comma "," is remove to only keep the base64 string
            // Image type: "data:image/png;base64,encodedimage"
            $scope.image.type = $scope.image.show ? $scope.image.croppedImage.slice($scope.image.croppedImage.indexOf(',')+1): null;
        }

        const author = $rootScope.userAuthenticated ? `${ $rootScope.userAuthenticated.first_name  },${  $rootScope.userAuthenticated.last_name  },${  $rootScope.userAuthenticated.email }` : 'unknown';
        // $scope.pin is only defined when the pin is updated (and not created), and in the update, this scope is passed to the controller in vboardPin.js
        const timeBeforeReload = 3000;
        if ($scope.pin) {
            vboardPinsCollection.updatePin($scope.pin.pinId, $scope.editablePin.title, $scope.editablePin.url, null, $scope.image.type, $scope.editablePin.description, $scope.editablePin.labels, author)
                .then(function () {
                    $scope.closeThisDialog('OK');
                    // If the timeout is too short, the update will not be taken into account in front
                    // The ELS needs to be updated in the back-end and then the app refresh all, otherwise the update will be made in less than 2 min (auto refresh)
                    $timeout(function () {
                        vboardPinsCollection.forceUpdate();
                    }, timeBeforeReload);
                }, function (error) {
                    $scope.error = error;
                    console.error('addPin ERROR', error);
                });
        } else {
            vboardPinsCollection.addPin($scope.editablePin.title, $scope.editablePin.url, null, $scope.image.type, $scope.editablePin.description, $scope.editablePin.labels, author)
                .then(function () {
                    $scope.closeThisDialog('OK');
                    // If the timeout is too short, the update will not be taken into account in front
                    $timeout(function () {
                        vboardPinsCollection.forceUpdate();
                    }, timeBeforeReload);
                }, function (error) {
                    $scope.error = error;
                    console.error('addPin ERROR', error);
                });
        }
    };

    // Update the pin preview field if it is an update, with the current pin parameters
    /* eslint-disable complexity */
    if ($scope.pin) {
        $scope.editablePin.title = $scope.pin.pinTitle;
        // Replace break lines
        $scope.editablePin.description = $scope.pin.indexableTextContent.replace(/<br \/>/g, "\n");
        $scope.editablePin.descriptionShow = $scope.pin.indexableTextContent;
        if ($scope.pin.imgType) {
            $scope.image.show = true;
            // If the link contains <iframe, the app put it as a vidéo (set videoLink)
            if ($scope.pin.imgType.startsWith('<iframe')) {
                const scr = $scope.pin.imgType.substring($scope.pin.imgType.indexOf('src') + 5);
                // 5: to remove the src=" and only keep what is inside the src (what is after here, and only what is inside the src in the next line
                $scope.videoLink = $sce.trustAsResourceUrl(scr.substring(0, scr.indexOf('"'))); // trust the url, the iframe is recreating in html to avoid injection
            }
        }
        $scope.image.croppedImage = $scope.pin.imgType;
        $scope.editablePin.labels = $scope.pin.labels ? $scope.pin.labels.split(','): [];
        $scope.editablePin.url = $scope.pin.hrefUrl;
        $scope.editablePin.date = moment($scope.pin.postDateUTC).fromNow();
        $scope.popupTitle = "Modification d'épingle";
        if ($scope.pin.author.split(',').length === 3) {
            $scope.editablePin.author = $scope.pin.author.split(',').length === 3 ? `${ $scope.pin.author.split(',')[0]  } ${  $scope.pin.author.split(',')[1] }` : $scope.pin.author;
            $scope.editablePin.avatar = $scope.pin.author.split(',').length === 3 ? `/avatar/${  $scope.pin.author.split(',')[2]  }.png`: $scope.pin.author;
        }
    }

    // Replace break lines in preview
    $scope.$watch('editablePin.description', function () {
        $scope.editablePin.descriptionShow = $scope.editablePin.description ? $scope.editablePin.description.replace(/\n/g, "<br />"): '';
    });

    $scope.closeAddPinDialog = function () {
        ngDialog.close();
    };


    /** Media */

    $scope.removeImage = function () {
        $scope.image.originalImage = null;
        $scope.image.croppedImage = null;
        $scope.image.show = false;
        $scope.showCrop = false;
    };

    $scope.addImageURL = function () {
        if ($scope.image.croppedImage && $scope.image.croppedImage.startsWith('http')) {
            $scope.image.show = true;
        }
        if ($scope.image.croppedImage && $scope.image.croppedImage.startsWith('<iframe')) {
            $scope.image.show = true;
            /* eslint-disable camelcase */
            const scrUrl = $scope.image.croppedImage.substring($scope.image.croppedImage.indexOf('src') + 5);
            $scope.videoLink = $sce.trustAsResourceUrl(scrUrl.substring(0, scrUrl.indexOf('"')));
        }
    };

    $scope.uploadFile = function (file) {
        $scope.showCrop = true;
        if (file) {
            // ui-cropper
            const imageReader = new FileReader();
            imageReader.onload = function (image) {
                /* eslint-disable no-shadow */
                $scope.$apply(function ($scope) {
                    $scope.image.originalImage = image.target.result;
                    $scope.image.show = true;
                });
            };
            imageReader.readAsDataURL(file);
        }
    };

    $scope.$watch('image.croppedImage', function (newValue, oldValue) {
        // The following string match an empty image, which is being updated by the cropping
        // This watch allow the user to preview the image of the updated pin without it being overwritten by the cropped one (the empty cropped one).
        const emptyImg = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAMgAAADICAYAAACtWK6eAAAAsUlEQVR4nO3BAQEAAACCIP+vbkhAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB8GXHmAAFMgHIEAAAAAElFTkSuQmCC";
        if ($scope.image.show && newValue === emptyImg && oldValue !== newValue) {
            $scope.image.croppedImage = oldValue;
        }
    });


    /** Scrapping */
    $scope.getInfoFromURL = function (url) {
        if (url && url.startsWith("http")) {
            $http.post(`${ CONFIG.apiEndpoint  }/pins/url/`, {
                urlinfo: url
            }).then(function (response) {
                if (response.status !== 200) {
                    throw new Error(`User search failed:${  JSON.stringify(response) }`);
                }
                if (!$scope.editablePin.title) {
                    $scope.editablePin.title = response.data.title;
                }
                if (!$scope.image.show && response.data.image) {
                    $scope.image.show = true;
                    $scope.image.croppedImage = response.data.image;
                }
                if (!$scope.editablePin.description) {
                    $scope.editablePin.description = response.data.description;
                }
            });
        }
    };

});
