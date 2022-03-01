package edu.nd.crc.paperanalyticaldevices.api;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vdurmont.semver4j.Semver;

import org.junit.Test;
import org.opencv.dnn.Net;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class NetworkEntryTest {
    @Test
    public void deserializes_single_correctly() throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Semver.class, new NetworkEntry.SemverDeserializer())
                .registerTypeAdapter(TypeToken.getParameterized(List.class, String.class).getType(), new NetworkEntry.StringListDeserializer())
                .create();
        Reader input = Files.newBufferedReader(Paths.get(getClass().getClassLoader().getResource("network_entry.json").getPath()));
        NetworkEntry result = gson.fromJson(input, NetworkEntry.class);

        assertEquals("6d6f72b0ddfd8aa58672b3b1fd60a0c3fa8ef628a3bbe1383a083ee70c915121", result.Hash);
        assertEquals("PLSD method using 10 sections per lane.", result.Description);
        assertEquals(Arrays.asList("Albendazole", "Amoxicillin", "Ampicillin", "Azithromycin", "Benzyl", "Penicillin", "Ceftriaxone", "Chloroquine", "Ciprofloxacin", "Doxycycline", "Epinephrine", "Ethambutol", "Ferrous", "Sulfate", "Hydroxychloroquine", "Isoniazid", "Promethazine", "Hydrochloride", "Pyrazinamide", "Rifampicin", "RIPE", "Sulfamethoxazole", "Tetracycline", "Distractor"), result.Drugs);
        assertEquals("pls_fhi360_conc", result.Name);
        assertEquals("pls", result.Type);
        assertEquals(new Semver("1.0", Semver.SemverType.LOOSE), result.Version);
        assertEquals("https://pad.crc.nd.edu/neuralnetworks/pls/fhi360_concentration/1.0/pls_fhi360_conc_coefficients.csv", result.Weights);
    }

    @Test
    public void deserializes_list_correctly() throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Semver.class, new NetworkEntry.SemverDeserializer())
                .registerTypeAdapter(TypeToken.getParameterized(List.class, String.class).getType(), new NetworkEntry.StringListDeserializer())
                .create();
        Reader input = Files.newBufferedReader(Paths.get(getClass().getClassLoader().getResource("network_entry_list.json").getPath()));
        ResponseList<NetworkEntry> result = gson.fromJson(input, TypeToken.getParameterized(ResponseList.class, NetworkEntry.class).getType());

        assertEquals("ok", result.Status);
        assertEquals(10, result.Entries.size());

        assertEquals("6d6f72b0ddfd8aa58672b3b1fd60a0c3fa8ef628a3bbe1383a083ee70c915121", result.Entries.get(9).Hash);
        assertEquals("PLSD method using 10 sections per lane.", result.Entries.get(9).Description);
        assertEquals(Arrays.asList("Albendazole", "Amoxicillin", "Ampicillin", "Azithromycin", "Benzyl", "Penicillin", "Ceftriaxone", "Chloroquine", "Ciprofloxacin", "Doxycycline", "Epinephrine", "Ethambutol", "Ferrous", "Sulfate", "Hydroxychloroquine", "Isoniazid", "Promethazine", "Hydrochloride", "Pyrazinamide", "Rifampicin", "RIPE", "Sulfamethoxazole", "Tetracycline", "Distractor"), result.Entries.get(9).Drugs);
        assertEquals("pls_fhi360_conc", result.Entries.get(9).Name);
        assertEquals("pls", result.Entries.get(9).Type);
        assertEquals(new Semver("1.0", Semver.SemverType.LOOSE), result.Entries.get(9).Version);
        assertEquals("https://pad.crc.nd.edu/neuralnetworks/pls/fhi360_concentration/1.0/pls_fhi360_conc_coefficients.csv", result.Entries.get(9).Weights);
    }
}
