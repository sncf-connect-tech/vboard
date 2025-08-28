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

"use strict";

// Recipe from: http://www.thoughtworks.com/insights/blog/using-page-objects-overcome-protractors-shortcomings
var AngularPage = function (searchPath) {
    browser.get('/vboard/#/' + (searchPath || ''));
    waitForEvent('vboardPinsCollectionUpdated');
};

AngularPage.prototype = Object.create({}, {
    // Getters
    toolbar: { get: function () {
        return $('vboard-toolbar');
    }},
    toolbarSearchInput: { get: function () {
        return this.toolbar.$('.vboardToolbar--textSearch > input');
    }},
    toolbarLabels: { get: function () {
        return this.toolbar.$$('.vboardLabels--label');
    }},
    toolbarLabelNames: { get: function () {
        return this.toolbarLabels.$$('.vboardLabels--labelName').map(function (elem) {
            return elem.getText();
        });
    }},
    toolbarLabelCloseAnchors: { get: function () {
        return this.toolbarLabels.$$('.vboardLabels--labelBoxClose > a');
    }},
    toolbarLabelInput: { get: function () {
        return this.toolbar.$('input.vboardLabels--search');
    }},
    toolbarNewPinButton: { get: function () {
        return this.toolbar.$$('.vboardToolbar--button').get(0);
    }},
    addPinDialogLabels: { get: function () {
        return $$('.vboardAddPinDialog .vboardLabels--label');
    }},
    addPinDialogLabelNames: { get: function () {
        return this.addPinDialogLabels.$$('.vboardLabels--labelName').map(function (elem) {
            return elem.getText();
        });
    }},
    addPinDialogLabelCloseAnchors: { get: function () {
        return this.addPinDialogLabels.$$('.vboardLabels--labelBoxClose > a');
    }},
    pins: { get: function () {
        return $$('.vboardPin');
    }},
    pinTweets: { get: function () {
        return $$('.vboardPin--tweet');
    }},
    pinTweetIframes: { get: function () {
        return $$('iframe.twitter-tweet-rendered');
    }},

    // Setters
    setLocation: { value: function (newLocation) {
        browser.setLocation(newLocation);
        waitForEvent('vboardPinsCollectionUpdated');
    }},
    createToolbarLabel: { value: function (tagName) {
        this.toolbarLabelInput.sendKeys(tagName);
        this.toolbarLabelInput.sendKeys(protractor.Key.ENTER);
    }},
    removeFirstLabelByClicking: { value: function () {
        this.toolbarLabelCloseAnchors.first().click();
    }},
    pressEscape: { value: function () {
        $('body').sendKeys(protractor.Key.ESCAPE);
    }}
});

function waitForEvent(ngEventName) {
    browser.executeScript("window." + ngEventName + "_triggered = false;");
    browser.executeScript("\
        $(document.body).scope().$on('" + ngEventName + "', function () {\
            window." + ngEventName + "_triggered = true;\
        });\
    ");
    browser.executeScript("window." + ngEventName + "_triggered = false;");
}

module.exports = AngularPage;