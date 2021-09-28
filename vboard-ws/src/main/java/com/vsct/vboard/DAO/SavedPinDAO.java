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

import com.vsct.vboard.models.SavedPin;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Transactional
public interface SavedPinDAO extends CrudRepository<SavedPin, String> {
    // No implementation needed
    Optional<SavedPin> findById(String id); // Spring will generate an implementation based on the method name: findBy+attributeName

    List<SavedPin> findByUserEmail(String user_email); // Spring will generate an implementation based on the method name: findBy+attributeName

    List<SavedPin> findByPinId(String pin_id); // Spring will generate an implementation based on the method name: findBy+attributeName
}
