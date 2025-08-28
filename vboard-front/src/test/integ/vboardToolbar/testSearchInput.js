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

    it("can set the text to search for through the url location", function () {
        page.setLocation('?text=DummyText');

        expect(page.toolbarSearchInput.getAttribute('value')).toBe('DummyText');
    });

    it("can set the text to search for through the url location", function () {
        page.toolbarSearchInput.sendKeys('something');

        expect(page.toolbarSearchInput.getAttribute('value')).toBe('something');

        page.setLocation('?text=');

        expect(page.toolbarSearchInput.getAttribute('value')).toBe('');
    });
});
