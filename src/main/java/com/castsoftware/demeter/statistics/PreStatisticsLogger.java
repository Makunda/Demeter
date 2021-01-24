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

package com.castsoftware.demeter.statistics;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.demeter.StatisticNode;
import com.castsoftware.demeter.statistics.Highlights.Highlight;
import com.castsoftware.demeter.statistics.Highlights.HighlightCategory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreStatisticsLogger implements AutoCloseable {

  private static final String FILE_EXTENSION = Configuration.get("pre_statistics.file.extension");
  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HHmmss");
  private static final SimpleDateFormat cdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

  private String applicationContext;
  private FileWriter file;

  private StringBuilder buffer = new StringBuilder();

  /**
   * Constructor
   *
   * @param applicationContext Name of the application concerned by these statistics
   * @throws IOException If the PreStatisticsLogger failed to create the statistics file
   */
  public PreStatisticsLogger(String applicationContext) throws IOException {
    String outputDirectory =
        Configuration.get("demeter.workspace.path")
            + Configuration.get("demeter.workspace.statistics.file.path");
    File statisticsDir = new File(outputDirectory);

    if (!statisticsDir.exists()) {
      statisticsDir.mkdirs();
    }

    this.applicationContext = applicationContext;

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    String forgedPath =
        String.format(
            "%s/Pre_Statistics_for_%s_on%s.%s",
            outputDirectory, applicationContext, sdf.format(timestamp), FILE_EXTENSION);
    file = new FileWriter(forgedPath);

    flushBuffer();
  }

  /**
   * Get output directory where the pre-statistics files are saved
   *
   * @return The path of the directory
   */
  public static String getOutputDirectory() {
    return Configuration.get("demeter.workspace.path")
        + Configuration.get("demeter.workspace.statistics.file.path");
  }

  public void writeIntro() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    String title =
        String.format(
            "Report generated on %s for application %s . It contains pre-tagging statistics",
            cdf.format(timestamp), applicationContext);

    String box = "#".repeat(124) + String.format("\n# %1$-120s #\n", title) + "#".repeat(124);

    buffer.append(box);
  }

  public void writeParagraph(String title, String para) {
    buffer.append(String.format("\n%s %-36s %s\n", "-".repeat(36), title, "-".repeat(50)));
    buffer.append(para);
  }

  private void writeEndOfSection() {
    buffer.append("#".repeat(124));
  }

  public void writeStatistics(List<StatisticNode> statistics) {
    String title = String.format("\nStatistics for application :  %s \n", applicationContext);
    buffer.append(title);

    for (StatisticNode stn : statistics) {
      StringBuilder statRes = new StringBuilder();
      statRes.append("-".repeat(124)).append("\n");
      try {
        statRes.append("\n\tStatistics on : ").append(stn.getName()).append("\n");
        String description = stn.getDescription();
        if (!description.isEmpty()) {
          statRes
              .append(" \tDescription : ")
              .append(stn.getDescription().replaceAll("\\n", "\n\t"))
              .append("\n\n");
        }

        statRes.append("\tResults of the statistics : \n");
        String res = stn.executeStat(applicationContext);
        statRes.append(res);
      } catch (Neo4jBadRequestException | Neo4jNoResult | Exception | Neo4jQueryException e) {
        statRes.append("\nAn error occurred during the execution of this statistic.\n");
        statRes.append(e.getMessage()).append("\n");
      }
      buffer.append(statRes);
    }
    buffer.append("-".repeat(124)).append("\n");

    writeEndOfSection();
  }

  public void writeHighlights(List<Highlight> highlights) {
    Map<HighlightCategory, String> sortedCases = new HashMap<>();

    buffer.append(String.format("\n%d tags were processed in this scan.\n", highlights.size()));

    for (Highlight h : highlights) {

      String description = h.getDescription();
      if (description.isEmpty()) description = "Description not available.";

      // Forge the line
      String line = String.format("\t- %-18s | %s", h.getType().getText(), h.getTitle());
      line += "\n\n\tNumber of occurrence in the application : " + h.getFindings();
      line += "\n\tUse case addressed : " + h.getUseCaseTitle();
      line += "\n\tDescription : " + description;
      line += "\n\n";

      String value = sortedCases.getOrDefault(h.getCategory(), "\n");
      value += line;
      sortedCases.put(h.getCategory(), value);
    }

    for (Map.Entry<HighlightCategory, String> entry : sortedCases.entrySet()) {
      writeParagraph(entry.getKey().getText(), entry.getValue());
    }

    writeEndOfSection();
  }

  public String getBufferState() {
    return buffer.toString();
  }

  public void flushBuffer() {
    buffer = new StringBuilder();
    writeIntro();
  }

  /**
   * Write to file and flush buffer.
   *
   * @throws IOException
   */
  public void save() throws IOException {
    file.write(buffer.toString());
    file.flush();
    flushBuffer();
  }

  @Override
  public void close() throws Exception {
    if (file != null) {
      file.close();
    }
  }
}
