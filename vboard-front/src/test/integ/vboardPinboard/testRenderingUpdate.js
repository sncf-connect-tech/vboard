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

"use strict";

var AngularPage = require('../AngularPage');

describe("A user wanting to update the board", function () {
    var page;
    beforeEach(function () {
        page = new AngularPage('?corked&labels=%23python');
    });

    it("can find several pins for label '#python' in 'corked' test mode", function () {
        expect(page.pins.count()).not.toEqual(0);
    });

    it("can add two labels, remove one, and the board won't ever be empty of pins", function () {
        page.removeFirstLabelByClicking();

        expect(page.pins.count()).not.toEqual(0);
    });

    it("can add two labels, remove one, and the board won't ever be empty of tweets", function () {
        expect(page.pinTweets.count()).not.toEqual(0);

        page.removeFirstLabelByClicking();

        expect(page.pinTweets.count()).not.toEqual(0);
    });

    it("can add two labels, remove one, and the board won't be ever contain duplicate tweet iframes", function () {
        expect(page.pinTweetIframes.count()).not.toEqual(0);

        page.removeFirstLabelByClicking();

        expect(page.pinTweetIframes.count()).toEqual(page.pinTweets.count());
    });
});
