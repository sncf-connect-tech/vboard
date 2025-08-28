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
        page = new AngularPage('?labels=DummyTag');
    });

    it("can add a label through the url location", function () {
        expect(page.toolbarLabelNames).toEqual(['DummyTag']);
    });

    it("can add labels by clicking", function () {
        page.createToolbarLabel('YummyDad');

        expect(page.toolbarLabelNames).toEqual(['DummyTag', 'YummyDad']);
    });

    it("can delete a label by click", function () {
        page.createToolbarLabel('YummyDad');
        page.removeFirstLabelByClicking();

        expect(page.toolbarLabelNames).toEqual(['YummyDad']);

        page.removeFirstLabelByClicking();

        expect(page.toolbarLabels.count()).toBe(0);
    });

    it("can delete a label through the url location", function () {
        page.setLocation('?labels=');

        expect(page.toolbarLabels.count()).toBe(0);

        page.setLocation('?labels=YummyDad');

        expect(page.toolbarLabelNames).toEqual(['YummyDad']);
    });
});
