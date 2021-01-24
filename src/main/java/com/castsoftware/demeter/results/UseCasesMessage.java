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

package com.castsoftware.demeter.results;

import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.demeter.UseCaseNode;

public class UseCasesMessage {
  public String name;
  public String parentName;
  public Boolean active;
  public Long id;

  public UseCasesMessage(String name, Boolean active, Long id) {
    super();
    this.name = name;
    this.active = active;
    this.id = id;
  }

  public UseCasesMessage(UseCaseNode n)
      throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
    super();
    this.name = n.getName();
    this.active = n.getActive();
    this.id = n.getNodeId();
    this.parentName = n.getParentUseCase();
  }
}
