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

package com.vsct.vboard.DAO;


import com.vsct.vboard.models.Comment;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
public interface CommentDAO extends CrudRepository<Comment, String> {
    // No implementation needed
    List<Comment> findByPin(String pin); // Spring will generate an implementation based on the method name: findBy+attributeName

    List<Comment> findByAuthor(String author); // Spring will generate an implementation based on the method name: findBy+attributeName

    Comment findById(String id); // Spring will generate an implementation based on the method name: findBy+attributeName
}
