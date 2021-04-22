package com.gazbert.bxbot.strategies.ta4jhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ta4j.core.BarSeries;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonBarsSerializer {

    private static final Logger LOG = Logger.getLogger(JsonBarsSerializer.class.getName());
    private static Map<String, GsonBarSeries> cachedSeries = new HashMap<>();

    public static void persistSeries(BarSeries series, String filename) {
        GsonBarSeries exportableSeries = GsonBarSeries.from(series);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter writer = null;
        try {
            writer = new FileWriter(filename);
            gson.toJson(exportableSeries, writer);
            LOG.info("Bar series '" + series.getName() + "' successfully saved to '" + filename + "'");
        } catch (IOException e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "Unable to store bars in JSON", e);
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static BarSeries loadSeries(String filename) {
        if (cachedSeries.containsKey(filename)) {
            return cachedSeries.get(filename).toBarSeries();
        }
        Gson gson = new Gson();
        FileReader reader = null;
        BarSeries result = null;
        try {
            reader = new FileReader(filename);
            GsonBarSeries loadedSeries = gson.fromJson(reader, GsonBarSeries.class);
            cachedSeries.put(filename, loadedSeries);
            result = loadedSeries.toBarSeries();
            LOG.info("Bar series '" + result.getName() + "' successfully loaded. #Entries: " + result.getBarCount());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "Unable to load bars from JSON", e);
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
