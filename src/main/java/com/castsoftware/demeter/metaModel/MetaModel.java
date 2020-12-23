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

package com.castsoftware.demeter.metaModel;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MetaModel {

    private final static String TEMPLATE_DEFAULT_NAME = "Template";
    private final static String DEFAULT_EXTENSION = ".json";

    private final Neo4jAL neo4jAL;
    private final Path metaModelPath;
    private final String applicationContext;

    /**
     * Split the nodes not belonging to a transaction.
     * @throws Neo4jQueryException
     */
    private void separateNotInTransactionsObjects() throws Neo4jQueryException {
        String forgedRequest = String.format("MATCH (l:Level5:%1$s)-[:Aggregates]->(obj:Object:%1$s) " +
                "WHERE obj.External=false AND NOT (:Transaction:%1$s)-[:Contains]->(obj:Object:%1$s) "+
                "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['Dm_gl_Not in transaction '+l.Name] ELSE  [x in  obj.Tags WHERE NOT x CONTAINS 'Dm_gl_'] + ('Dm_gl_Not in transaction '+l.Name) END", applicationContext);
        neo4jAL.executeQuery(forgedRequest);
    }

    /**
     * Split the nodes flagged as external from the rest of the application
     * @throws Neo4jQueryException
     */
    private void splitExternalsObjects() throws Neo4jQueryException {
        String forgedRequest = String.format("MATCH (l:Level5:%1$s)-[:Aggregates]->(obj:Object:%1$s) " +
                "WHERE obj.External=true " +
                "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['Dm_gl_External '+l.Name] ELSE [x in  obj.Tags WHERE NOT x CONTAINS 'Dm_gl_'] + ('Dm_gl_External '+l.Name) END", applicationContext);
        neo4jAL.executeQuery(forgedRequest);
    }

    /**
     * Merge object by type
     * @param types Type to be merged
     * @throws Neo4jQueryException
     */
    private void mergeObjectByType(String[] types) throws Neo4jQueryException {
        String forgedRequest;
        for (int i = 0; i < types.length; i++) {
            forgedRequest = String.format("MATCH (obj:Object:%1$s) " +
                    "WHERE obj.Type='%2$s' " +
                    "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['Dm_gl_Extracted'+obj.Type] ELSE [x in  obj.Tags WHERE NOT x CONTAINS 'Dm_gl_'] + ('Dm_gl_Extracted '+obj.Type) END", applicationContext, types[i]);
            neo4jAL.executeQuery(forgedRequest);
        }
    }

    /**
     * Process the content of a Metamodel file
     * @throws IOException
     */
    public void process() throws IOException {
        Gson gson = new Gson();
        MetaModelStructure metaModel;
        try(JsonReader reader = new JsonReader(new FileReader(metaModelPath.toAbsolutePath().toString()))) {
            metaModel = gson.fromJson(reader, MetaModelStructure.class);
        }

        // Separate Transactions
        if(metaModel.splitNotInTransactionsObjects) {
            try {
                separateNotInTransactionsObjects();
            } catch (Neo4jQueryException e) {
                neo4jAL.logError("Failed to execute objects split by their presence in transactions.", e);
            }
        }

        // Split Externals
        if(metaModel.splitExternalObjects) {
            try {
                splitExternalsObjects();
            } catch (Neo4jQueryException e) {
                neo4jAL.logError("Failed to execute objects split by their externality.", e);
            }
        }

        if(metaModel.toMergeObjectType.length != 0) {

        }
    }

    /**
     * Generate a default metamodel to be used as a template by the user
     * @param outputFilePath Output directory
     * @throws IOException If a template file already exists at the specified path or the permission aren't sufficient to write.
     */
    public static Path generateTemplateMetaModel(Path outputFilePath) throws IOException {
        Path fullPath = outputFilePath.resolve(TEMPLATE_DEFAULT_NAME.concat(DEFAULT_EXTENSION));

        if(Files.exists(fullPath)) {
            throw new FileAlreadyExistsException(String.format("A file already exists with the same name '%s'. Cannot create Template.", outputFilePath.toString()));
        }

        MetaModelStructure mms = new MetaModelStructure();

        try (FileWriter fw = new FileWriter(fullPath.toAbsolutePath().toString())) {
            new Gson().toJson(mms, fw);
        }

        return fullPath;
    }

    /**
     * Constructor
     * @param neo4jAL Neo4j access layer
     * @param metaModelName Name of the metamodel to execute
     * @param applicationContext Name of the application
     * @throws FileNotFoundException The metamodel name doesn't exists in the Demeter workspace
     */
    public MetaModel(Neo4jAL neo4jAL, String  metaModelName, String applicationContext) throws FileNotFoundException {
        Path fullPath = Path.of(Configuration.get("demeter.workspace.path") + Configuration.get("meta.model.path"));
        Path metaModelFile = fullPath.resolve(metaModelName.concat(DEFAULT_EXTENSION));

        if (!Files.exists(metaModelFile)) {
            throw new FileNotFoundException(String.format("The meta-model file with name '%s' in folder '%s'.", metaModelName.concat(DEFAULT_EXTENSION), fullPath));
        }

        this.neo4jAL = neo4jAL;
        this.metaModelPath = metaModelFile;
        this.applicationContext = applicationContext;
    }
}
