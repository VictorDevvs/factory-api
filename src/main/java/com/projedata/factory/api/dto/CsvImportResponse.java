package com.projedata.factory.api.dto;

import java.util.List;

public record CsvImportResponse (
        int recordsImported,
        int recordsSkipped,
        List<String> errors
){
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
