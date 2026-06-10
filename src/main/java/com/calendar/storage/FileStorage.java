package com.calendar.storage;

import com.calendar.model.Event;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileStorage {

    private final File file;
    private final ObjectMapper mapper;

    public FileStorage(String filePath) {
        this.file = new File(filePath);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<Event> loadEvents() {
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(file, new TypeReference<List<Event>>() {});
        } catch (IOException e) {
            System.err.println("Warning: Could not read events file — starting with empty calendar.");
            return new ArrayList<>();
        }
    }

    public void saveEvents(List<Event> events) {
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            mapper.writeValue(file, events);
        } catch (IOException e) {
            System.err.println("Error: Could not save events — " + e.getMessage());
        }
    }
}
