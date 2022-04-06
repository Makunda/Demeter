/*
 * Copyright (C) 2020  Hugo JOBY
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License v3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public v3
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.demeter.results.backup;

import com.castsoftware.demeter.models.backup.MasterSaveNode;

public class MasterSaveResult {
    public Long id;
    public String name;
    public String description;
    public Long timestamp;
    public String picture;

    public MasterSaveResult(MasterSaveNode node) {
        this.id = node.getId();
        this.name = node.getName();
        this.description = node.getDescription();
        this.timestamp = node.getTimestamp();
        this.picture = node.getPicture();
    }

}
