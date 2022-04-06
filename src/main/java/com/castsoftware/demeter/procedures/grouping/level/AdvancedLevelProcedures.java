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

package com.castsoftware.demeter.procedures.grouping.level;

import com.castsoftware.demeter.controllers.grouping.levels.AdvancedLevelGrouping;
import com.castsoftware.demeter.controllers.grouping.levels.LevelGroupController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.NodeResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class AdvancedLevelProcedures {


    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "demeter.group.with.category", mode = Mode.WRITE)
    @Description(
            "demeter.group.with.category(String applicationName, String category, String name, List<Long> idList) - Group with a level 3 category")
    public Stream<NodeResult> groupLevels(@Name(value = "Application") String application,
                                          @Name(value = "Category") String category,
                                          @Name(value = "Name") String name,
                                          @Name(value = "IdList") List<Long> idList)
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            AdvancedLevelGrouping alg = new AdvancedLevelGrouping(nal);
            List<Node> nodes = alg.groupWithCategory(application, category, name, idList);

            return nodes.stream().map(NodeResult::new);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.group.with.taxonomy", mode = Mode.WRITE)
    @Description(
            "demeter.group.with.taxonomy(String applicationName, String level1,  String level2, " +
                    "String level3,  String level4,  String level5, List<Long> idList) " +
                    "- Group with the full cast taxonomy")
    public Stream<NodeResult> groupLevels(@Name(value = "Application") String application,
                                          @Name(value = "Level1") String level1,
                                          @Name(value = "Level2") String level2,
                                          @Name(value = "Level3") String level3,
                                          @Name(value = "Level4") String level4,
                                          @Name(value = "Level5") String level5,
                                          @Name(value = "IdList") List<Long> idList)
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            AdvancedLevelGrouping alg = new AdvancedLevelGrouping(nal);
            List<Node> nodes = alg.groupWithTaxonomy(application, level1, level2, level3, level4, level5, idList);

            return nodes.stream().map(NodeResult::new);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }


}
