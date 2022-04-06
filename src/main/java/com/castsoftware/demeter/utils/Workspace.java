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

package com.castsoftware.demeter.utils;

import com.castsoftware.demeter.config.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Workspace {

    /**
     * Get the supposed path of the initialization zip
     *
     * @return
     */
    public static Path getInitDataZip() {
        String workspace = Configuration.get("demeter.workspace.path");
        Path dataFolder = Path.of(workspace).resolve(Configuration.get("demeter.workspace.data.folder"));
        return dataFolder.resolve(Configuration.get("demeter.workspace.data.zip_file"));
    }

    /**
     * Get the path of the User configuration file
     *
     * @return
     */
    public static Path getUserConfigPath() {
        String workspace = Configuration.get("demeter.workspace.path");
        return Path.of(workspace).resolve(Configuration.get("demeter.workspace.data.user_configuration"));
    }

    /**
     * Get current workspace
     *
     * @return
     */
    public static Path getWorkspace() {
        String workspace = Configuration.get("demeter.workspace.path");
        return Path.of(workspace);
    }


    /**
     * Check if the folder exist. If not, create it
     *
     * @param folderPath Path of the folder to check
     * @param name       Name of the folder
     * @return
     */
    private static List<String> checkOrCreateFolder(Path folderPath, String name) {
        List<String> messageOutputList = new ArrayList<>();
        // Check main folders and create if necessary
        if (!Files.exists(folderPath)) {
            try {
                Files.createDirectory(folderPath);
                messageOutputList.add(String.format("%s was missing and has been created.", name));
            } catch (IOException e) {
                messageOutputList.add(
                        String.format(
                                "ERROR : %s is missing and its creation failed : %s", name, e.getMessage()));
            }
        }

        return messageOutputList;
    }

    /**
     * Validate if the workspace is valid and contains all the Artemis mandatory files
     *
     * @return List of message to be displayed
     */
    public static List<String> validateWorkspace() {
        List<String> messageOutputList = new ArrayList<>();

        String workspace = Configuration.get("demeter.workspace.path");
        Path workspacePath = Path.of(workspace);
        Path statisticsFolder =
                workspacePath.resolve(Configuration.get("demeter.workspace.statistics.file.path"));
        Path savesFolder = workspacePath.resolve(Configuration.get("demeter.workspace.save.folder"));
        Path dataFolder = workspacePath.resolve(Configuration.get("demeter.workspace.data.folder"));
        Path installData = dataFolder.resolve(Configuration.get("demeter.workspace.data.zip_file"));
        Path userConfigData = workspacePath.resolve(Configuration.get("demeter.workspace.data.user_configuration"));

        // Check if the folder is valid
        if (!Files.exists(workspacePath)) {
            messageOutputList.add(
                    String.format(
                            "ERROR : %s does not exist. Please specify an existing directory.", workspace));
            return messageOutputList;
        }

        // Check Statistic folder
        messageOutputList.addAll(checkOrCreateFolder(statisticsFolder, "Statistics folder"));

        // Check Saves folder
        messageOutputList.addAll(checkOrCreateFolder(savesFolder, "Save folder"));

        // Data Folder
        messageOutputList.addAll(checkOrCreateFolder(dataFolder, "Data folder"));

        // Check the existent of the user configuration file
        if (!Files.exists(installData)) {
            messageOutputList.add(
                    String.format(
                            "ERROR :  Data initialization zip '%s' is missing. The initialization will not work without this file.",
                            Configuration.get("demeter.workspace.data.zip_file")));
        }

        if (!Files.exists(userConfigData)) {
            messageOutputList.add(
                    String.format(
                            "ERROR :  User configuration data file '%s' is missing. The extension will not work without this file.",
                            Configuration.get("demeter.workspace.data.user_configuration")));
        }

        if (messageOutputList.isEmpty()) {
            messageOutputList.add("Demeter workspace is correctly configured.");
        }

        return messageOutputList;
    }
}
