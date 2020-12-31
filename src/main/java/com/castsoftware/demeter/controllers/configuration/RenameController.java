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

package com.castsoftware.demeter.controllers.configuration;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.BackupNode;
import com.castsoftware.demeter.models.imaging.Level5Node;
import org.neo4j.graphdb.*;

import java.util.Iterator;

public class RenameController {

    private static final String BACKED_UP_BY_REL = Configuration.get("demeter.backup.relationship.type");

    /**
     * Rename a specific level in the application. If two levels have the same name, rename the first encountered.
     * @param neo4jAL Neo4j Access Layer
     * @param applicationContext Name of the application concerned by this modification
     * @param toRename Name of the old level
     * @param newName Name of the new Level
     * @return true if the renaming operation was successful, false otherwise
     */
    public static boolean renameLevel(Neo4jAL neo4jAL, String applicationContext, String toRename, String newName)
            throws Neo4jNoResult, Neo4jQueryException {
        // Label backNodeLabel = Label.label(BackupNode.getLabel());
        // RelationshipType backedUpRelationship = RelationshipType.withName(BACKED_UP_BY_REL);

        // Find level to rename
        Level5Node found = null;
        for(Level5Node l : Level5Node.getAllNodesByApplication(neo4jAL, applicationContext)) {
            if(l.getName().equals(toRename)) {
                found = l;
                break;
            }
        }

        // If no level match, return false
        if(found == null) return false;

        Node levelNode = found.getNode();
        levelNode.setProperty(Level5Node.getNameProperty(), newName);

        return true;
    }

}
