package com.carbontrack.activity.dto;

import java.util.List;

/**
 * Result of a CSV upload. Valid rows are saved even when other rows fail.
 * {@code row} counts like a spreadsheet: row 1 is the header, the first data row is row 2.
 */
public record UploadResult(int imported, int failed, List<RowError> errors) {

    public record RowError(int row, String message) {
    }
}
