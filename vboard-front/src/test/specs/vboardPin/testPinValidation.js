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

/*eslint-disable camelcase*/
var VALID_DATUM = {
    pin_id: "tweet-598713611012460544",
    href_url: "https://twitter.com/bytesforall/status/598713611012460544",
    indexable_text_content: "\"How would you like this wrapped?\" https://imgur.com/nh7OBuG  #Netfreedom @APC_News @IFEX",
    labels: "#Netfreedom,@APC_News,@IFEX",
    author: 'obiwan_kenobi',
    post_date_utc: "2015-05-14T06:57:03.000+02:00"
};
/*eslint-enable camelcase*/

describe("vboardPin", function () {
    beforeEach(module('vboard'));

    var VboardPin;
    beforeEach(inject(['VboardPin', function (VboardPinClass) {
        VboardPin = VboardPinClass;
    }]));

    it("can detect missing fields", function () {
        expect(function () {
            new VboardPin(VALID_DATUM);
        }).not.toThrow();
    });

    it("can detect empty data", function () {
        expect(function () {
            new VboardPin(null);
        }).toThrowError(/Falsey datum provided/);
    });

    it("can detect forbidden empty fields", function () {
        var datum = _.clone(VALID_DATUM);
        /*eslint-disable camelcase*/
        datum.post_date_utc = "";
        /*eslint-enable camelcase*/
        expect(function () {
            new VboardPin(datum);
        }).toThrowError(/Forbidden empty field found/);
    });

    it("does not mind empty labels", function () {
        var datum = _.clone(VALID_DATUM);
        datum.labels = "";
        expect(function () {
            new VboardPin(datum);
        }).not.toThrow();
    });
});
