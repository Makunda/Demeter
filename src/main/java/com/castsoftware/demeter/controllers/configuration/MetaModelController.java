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

import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.metaModels.MetaModel;

import java.io.IOException;
import java.nio.file.Path;

public class MetaModelController {

    /**
     * Generate a template metamodel file.
     *
     * @param outputDir Path where the file will be generated
     * @return
     * @throws IOException
     */
    public static String generateTemplate(String outputDir) throws IOException {
        Path nf = MetaModel.generateTemplateMetaModel(Path.of(outputDir));
        return String.format("The template was successfully created : '%s'.", nf.toString());
    }

    /**
     * Execute a specific Metamodel file
     *
     * @param neo4jAL       Neo4j Access Layer
     * @param metaModelName Name of the meta model to execute.
     */
    public static void executeMetamodel(Neo4jAL neo4jAL, String applicationContext, String metaModelName) throws IOException {
        MetaModel mm = new MetaModel(neo4jAL, metaModelName, applicationContext); // Create the metamodel
        mm.process(); // Process the metamodel
    }

}
