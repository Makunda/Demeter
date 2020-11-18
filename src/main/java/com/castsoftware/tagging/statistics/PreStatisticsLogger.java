package com.castsoftware.tagging.statistics;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.models.StatisticNode;
import com.castsoftware.tagging.statistics.Highlights.Highlight;
import com.castsoftware.tagging.statistics.Highlights.HighlightCategory;

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
        buffer.append(String.format("\n%s %-36s %s\n", "-".repeat(36), title, "-".repeat(50)));
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
            statRes.append("-".repeat(124) + "\n");
            try {
                statRes.append("\n\tStatistics on : ").append(stn.getName()).append("\n");
                String description = stn.getDescription();
                if(!description.isEmpty()) {
                    statRes.append(" \tDescription : ")
                            .append(stn.getDescription().replaceAll("\\n", "\n\t"))
                            .append("\n\n");
                }

                statRes.append("\tResults of the statistics : \n");
                String res = stn.executeStat(applicationContext);
                statRes.append(res);
            }
            catch (Neo4jBadRequestException | Neo4jNoResult | Exception e) {
                statRes.append("\nAn error occurred during the execution of this statistic.\n");
                statRes.append(e.getMessage() + "\n");
            }
            buffer.append(statRes);
        }
        buffer.append("-".repeat(124) + "\n");

        writeEndOfSection();
    }

    public void writeHighlights(List<Highlight> highlights) {
        Map<HighlightCategory, String> sortedCases = new HashMap<>();

        buffer.append(String.format("\n%d tags were processed in this scan.\n", highlights.size()));

        for (Highlight h : highlights) {

            String description = h.getDescription();
            if(description.isEmpty()) description = "Description not available.";

            // Forge the line
            String line = String.format("\t- %-18s | %s", h.getType(), h.getTitle());
            line += "\n\n\tNumber of occurrence in the application : " + h.getFindings();
            line += "\n\tUse case addressed : " + h.getUseCaseTitle();
            line += "\n\tDescription : " + description;
            line += "\n\n";

            String value = sortedCases.getOrDefault(h.getCategory(), "\n");
            value += line;
            sortedCases.put(h.getCategory(), value);

        }

        for(Map.Entry<HighlightCategory, String> entry : sortedCases.entrySet()) {
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
