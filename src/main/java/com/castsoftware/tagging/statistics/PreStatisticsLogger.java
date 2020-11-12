package com.castsoftware.tagging.statistics;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.models.StatisticNode;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreStatisticsLogger implements AutoCloseable {
    private static final String FILE_LOCATION = Configuration.get("pre_statistics.file.path");
    private static final String FILE_EXTENSION = Configuration.get("pre_statistics.file.extension");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HHmmss");
    private static final SimpleDateFormat cdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    private String applicationContext;
    private FileWriter file;

    private StringBuilder buffer = new StringBuilder();

    public void writeIntro() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String title = String.format("Report generated on %s for application %s . It contains pre-tagging statistics", cdf.format(timestamp), applicationContext);

        String box = "#".repeat(124) +
                String.format("\n# %1$-120s #\n", title) +
                "#".repeat(124);

        buffer.append(box);
    }

    public void writeParagraph(String title, String para) {
        buffer.append(String.format("\n-------------------------- %-36s --------------------------\n", title));
        buffer.append(para);
    }

    private void writeEndOfSection() {
        buffer.append("#".repeat(124));
    }

    public void writeStatistics(List<StatisticNode> statistics) {
        String title = String.format("\nStatistics for application :  %s \n", applicationContext);
        buffer.append(title);

        for(StatisticNode stn : statistics) {
            StringBuilder statRes = new StringBuilder();
            statRes.append("-".repeat(40) + "\n");
            try {
                statRes.append("Statistics on : " + stn.getName() + "\n");
                String description = stn.getDescription();
                if(!description.isEmpty()) {
                    statRes.append("Description : " + stn.getDescription() + "\n");
                }

                statRes.append("Results of the statistic: \n");
                String res = stn.executeStat(applicationContext);
                statRes.append(res);
            }
            catch (Neo4jBadRequestException | Neo4jNoResult e) {
                statRes.append("An error occurred during the execution of this statistic.");
            }

            statRes.append("-".repeat(40) + "\n");
            buffer.append(statRes);
        }

        writeEndOfSection();
    }

    public void writeHighlights(List<Highlight> highlights) {
        Map<HighlightCategory, String> sortedCases = new HashMap<>();

        buffer.append(String.format("\n%d tags were processed in this scan.\n", highlights.size()));

        for (Highlight h : highlights) {

            String description = h.getDescription();
            if(description.isEmpty()) description = "Description not available.";

            // Forge the line
            String line = String.format("- %s", h.getTitle());
            line += "\nNumber of occurrence in the application : " + h.getFindings();
            line += "\nUse case addressed : " + h.getUseCaseTitle();
            line += "\nDescription : " + description;
            line += "\n\n";

            String value = sortedCases.getOrDefault(h.getCategory(), "\n");
            value += line;
            sortedCases.put(h.getCategory(), value);

        }

        for(Map.Entry<HighlightCategory, String> entry : sortedCases.entrySet()) {
            writeParagraph(entry.getKey().text, entry.getValue());
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
     * @throws IOException
     */
    public void save() throws IOException {
        file.write(buffer.toString());
        file.flush();
        flushBuffer();
    }


    public PreStatisticsLogger(String applicationContext) throws IOException {
        this.applicationContext = applicationContext;

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String forgedPath = String.format("%sPre_Statistics_for_%s_on%s.%s", FILE_LOCATION, applicationContext, sdf.format(timestamp), FILE_EXTENSION);
        file = new FileWriter(forgedPath);

        flushBuffer();
    }


    @Override
    public void close() throws Exception {
        if(file != null)  {
            file.close();
        }
    }
}
