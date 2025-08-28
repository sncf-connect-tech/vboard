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

var AngularPage = require('../AngularPage');

describe("The user manipulating the toolbar", function () {
    var page;
    beforeEach(function () {
        page = new AngularPage();
    });

    it("can open the 'new pin' dialog and its default labels will match the toolbar active ones", function () {
        page.setLocation('?labels=a,b');

        page.toolbarNewPinButton.click();

        expect(page.addPinDialogLabelNames).toEqual(page.toolbarLabelNames);
    });

    it("can open the 'new pin' dialog, remove a label, then close it and the toolbar labels won't have changed", function () {
        page.setLocation('?labels=a,b');

        page.toolbarLabelNames.then(function (initialLabelNames) {
            page.toolbarNewPinButton.click();
            page.addPinDialogLabelCloseAnchors.last().click();
            page.pressEscape();

            expect(page.toolbarLabelNames).toEqual(initialLabelNames);
        });
    });
});
