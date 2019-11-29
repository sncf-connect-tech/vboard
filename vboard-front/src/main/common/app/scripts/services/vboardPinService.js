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


angular.module('vboard').factory('VboardPin', function VboardPinFactory() {
    /* Constants = fields not nulls */
    const NON_EMPTY_FIELDS = ['pinId', 'postDateUTC'];
    /* Object VBoardPin */
    return function VboardPin(datum) {
        if (!datum) {
            throw new Error(`Falsey datum provided to create a Pin:${  datum }`);
        }
        const newPin = angular.extend(this, {
            pinId: datum.pin_id,
            pinTitle: datum.pin_title,
            hrefUrl: datum.href_url,
            likes: datum.likes,
            imgType: datum.img_type,
            indexableTextContent: datum.indexable_text_content.replace(/(?:\r\n|\r|\n)/g, '<br />'),
            labels: datum.labels,
            author: datum.author,
            postDateUTC: datum.post_date_utc,
            commentsNumber: datum.comments_number,
            lastComment: datum.last_comment
        });
        const newPinWithOnlyNonEmptyFields = _.pick(newPin, NON_EMPTY_FIELDS);
        if (_.some(newPinWithOnlyNonEmptyFields, _.isEmpty)) {
            throw new Error(`Forbidden empty field found during Pin validation: ${  _.findKey(newPinWithOnlyNonEmptyFields, _.isEmpty) }`);
        }
        return newPin;
    };
});