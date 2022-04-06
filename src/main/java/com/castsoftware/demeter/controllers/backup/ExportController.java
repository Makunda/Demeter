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

package com.castsoftware.demeter.controllers.backup;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExportController {

    private final Neo4jAL neo4jAL;
    private final String application;

    /**
     * Constructor
     *
     * @param neo4jAL     Neo4j Access Layer
     * @param application Name of the application
     */
    public ExportController(Neo4jAL neo4jAL, String application) {
        this.neo4jAL = neo4jAL;
        this.application = application;
    }

    /**
     * Export a Save
     *
     * @param application Name
     * @param path        Location to export the save file
     * @param packageFile Name of the package
     */
    public void exportState(String application, String path, String packageFile) throws Exception {
        // Get the name
        String databaseName = neo4jAL.getDb().databaseName();
        String part = databaseName.equals("imaging") ? "01" : "02";

        // Check if the path is valid
        Path directoryPath = Paths.get(path);
        if (!Files.exists(directoryPath))
            throw new Exception(String.format("The specified directory doesn't exist. Path : '%s'.", path));

        // Check if file exists
        String fileName = packageFile + ".bak_" + part;
        Path filePath = Path.of(path, fileName);
        if (Files.exists(filePath))
            throw new Exception(String.format("A file named '%s' already exists at path : '%s'.", packageFile, path));


        FileWriter fio = null;

        try {
            fio = new FileWriter(filePath.toFile());

            // Verify the type of the database
            if (databaseName.equals("imaging")) { // Retrieve information in the Imaging database
                this.exportImagingDatabase(fio);
            } else { // Retrieve information on the neo4j database

            }

            neo4jAL.logInfo(String.format("The database '%s' has been successfully exported.", databaseName));
        } catch (Exception e) {
            neo4jAL.logError(String.format("Failed to extract the database '%s'.", databaseName), e);
            throw new Exception("Export produced an exception. Please check the logs...");
        } finally {
            if (fio != null) fio.close();
        }
    }

    /**
     * Export the imaging database
     *
     * @param fio
     */
    private void exportImagingDatabase(FileWriter fio) {

    }


    public void importSave(Neo4jAL neo4jAL, String location) {
        // TODO
    }

}
