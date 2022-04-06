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

package com.castsoftware.demeter.controllers.imaging;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;

import java.util.Map;

public class ArchitectureController {

    private final Neo4jAL neo4jAL;

    public ArchitectureController(Neo4jAL neo4jAL) {
        this.neo4jAL = neo4jAL;
    }

    /**
     * Get the hidden prefix of Archimodel
     *
     * @return The prefix of the Hidden archimodel
     */
    public static String getHiddenArchimodelPrefix() {
        return Configuration.get("demeter.archimodel.hidden.label");
    }

    /**
     * Get the hidden prefix of Subset
     *
     * @return The prefix of the Hidden Subset
     */
    public static String getHiddenSubsetPrefix() {
        return Configuration.get("demeter.subset.hidden.label");
    }

    /**
     * Hide an architecture in the application
     * Hiding an architecture will automatically hide the children subset
     *
     * @param id Id of the module to hide
     */
    public void hideArchitectureById(Long id) throws Neo4jQueryException {
        String req = String.format("MATCH (m:ArchiModel) WHERE ID(m)=$IdNode " +
                "REMOVE m:ArchiModel SET m:`%1$s` " +
                "WITH m " +
                "MATCH (m)-[:Contains]->(s:Subset) " +
                "REMOVE s:Subset SET s:`%2$s` ", getHiddenArchimodelPrefix(), getHiddenSubsetPrefix());
        Map<String, Object> params = Map.of("IdNode", id);
        this.neo4jAL.executeQuery(req, params);
    }

    /**
     * Hide an susbset in the application
     *
     * @param id Id of the module to hide
     */
    public void hideSubsetById(Long id) throws Neo4jQueryException {
        String req = String.format("MATCH (m:Subset) WHERE ID(m)=$IdNode " +
                "REMOVE m:Subset SET m:`%1$s` ", getHiddenSubsetPrefix());
        Map<String, Object> params = Map.of("IdNode", id);
        this.neo4jAL.executeQuery(req, params);
    }

    /**
     * Display a subset in the application
     * Displaying a subset will automatically display the parent ArchiModel
     *
     * @param id Id of the module to hide
     */
    public void displaySubsetById(Long id) throws Neo4jQueryException {
        String req = String.format("MATCH (s:`%1$s`) WHERE ID(s)=$IdNode " +
                "REMOVE s:`%1$s` SET s:Subset " +
                "WITH s " +
                "MATCH (s)<-[:Contains]-(a:`%2$s`) " +
                "REMOVE a:`%2$s` SET a:ArchiModel ", getHiddenSubsetPrefix(), getHiddenArchimodelPrefix());
        Map<String, Object> params = Map.of("IdNode", id);
        this.neo4jAL.executeQuery(req, params);
    }

    /**
     * Display an architecture in the application and all the subset under it
     *
     * @param id Id of the module to hide
     */
    public void displayArchitectureWithChildrenById(Long id) throws Neo4jQueryException {
        String req = String.format("MATCH (m:`%1$s`) WHERE ID(m)=$IdNode " +
                "REMOVE m:`%1$s` SET m:ArchiModel " +
                "WITH m " +
                "MATCH (m)-[:Contains]->(s:`%2$s`) " +
                "REMOVE s:`%2$s` SET s:Subset ", getHiddenArchimodelPrefix(), getHiddenSubsetPrefix());
        Map<String, Object> params = Map.of("IdNode", id);
        this.neo4jAL.executeQuery(req, params);
    }

    /**
     * Display an Architecture by its id. The subset remain untouched
     *
     * @param id Id of the architecture
     * @throws Neo4jQueryException If the request is not correct
     */
    public void displayArchitectureById(Long id) throws Neo4jQueryException {
        String req = String.format("MATCH (m:`%1$s`) WHERE ID(m)=$IdNode " +
                "REMOVE m:`%1$s` SET m:ArchiModel ", getHiddenArchimodelPrefix());
        Map<String, Object> params = Map.of("IdNode", id);
        this.neo4jAL.executeQuery(req, params);
    }
}
