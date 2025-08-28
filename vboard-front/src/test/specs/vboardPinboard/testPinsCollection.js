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


const TEST_PINS = [
    {
        "pinId": "imgur-uastMqD",
        "type": "imgur",
        "labels": "",
        "hrefUrl": "http://imgur.com/r/programming/uastMqD",
        "postDateUTC": "2015-04-28T13:06:06.000+00:00",
        "description": "uastMqD",
        "imgUrl": "http://i.imgur.com/uastMqD.jpg"
    },
    {
        "pinId": "imgur-MM3RE8t",
        "type": "imgur",
        "labels": "",
        "hrefUrl": "http://imgur.com/r/programming/MM3RE8t",
        "postDateUTC": "2015-04-01T14:08:39.000+00:00",
        "description": "MM3RE8t",
        "imgUrl": "http://i.imgur.com/MM3RE8t.jpg"
    },
    {
        "pinId": "imgur-Q5QXBhj",
        "type": "imgur",
        "labels": "",
        "hrefUrl": "http://imgur.com/r/programming/Q5QXBhj",
        "postDateUTC": "2015-04-06T07:34:38.000+00:00",
        "description": "Q5QXBhj",
        "imgUrl": "http://i.imgur.com/Q5QXBhj.jpg"
    }
];

describe("vboardPinsCollection", function () {
    beforeEach(module('vboard'));

    let pinsCollection = null;
    beforeEach(inject(''));
    beforeEach(inject(['vboardPinsCollection',
        function (vboardPinsCollection) {
            pinsCollection = vboardPinsCollection;
            pinsCollection.allPins = TEST_PINS;
        }]));

    it("can sort the pins by postDate", function () {
        pinsCollection.replacePinsAndLabels();
        const aprilPins = pinsCollection.pins;
        expect(_.first(aprilPins).description).toBe('uastMqD');
        expect(_.last(aprilPins).description).toBe('Q5QXBhj');
    });
});
