package com.carbontrack.activity;

import com.carbontrack.activity.dto.ActivityRequest;
import com.carbontrack.activity.dto.UploadResult;
import com.carbontrack.activity.dto.UploadResult.RowError;
import com.carbontrack.common.BadRequestException;
import com.carbontrack.common.Category;
import com.carbontrack.common.NotFoundException;
import com.carbontrack.facility.Facility;
import com.carbontrack.facility.FacilityRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Imports activity records from a CSV file with the header {@code facilityName,category,quantity,year,month}.
 *
 * <p>Each row is validated and saved on its own, with the same rules as a manual record, and gets
 * {@code source = CSV}. A bad row is reported and skipped. It never blocks the good rows.
 * This class is deliberately not transactional: every row commits or fails by itself.
 *
 * <p>A row is a duplicate when the company already has a record with the same facility, category, year
 * and month (see {@link RecordKey}), either from earlier or from an earlier row of the same file. Duplicates
 * are reported as row errors and skipped, so re-uploading a file imports nothing the second time.
 * Manual entry is not affected: two meters in one month are legitimate there.
 */
@Service
public class CsvImportService {

    public static final int MAX_ROWS = 2000;
    static final List<String> REQUIRED_HEADERS = List.of("facilityName", "category", "quantity", "year", "month");

    private final ActivityService activities;
    private final FacilityRepository facilities;
    private final Validator validator;

    public CsvImportService(ActivityService activities, FacilityRepository facilities, Validator validator) {
        this.activities = activities;
        this.facilities = facilities;
        this.validator = validator;
    }

    public UploadResult importCsv(Long companyId, Long userId, InputStream in) {
        List<CSVRecord> rows = readRows(in);
        Map<String, List<Facility>> byName = facilitiesByName(companyId);
        Set<RecordKey> seen = new HashSet<>(activities.existingKeys(companyId));   // loaded once, grows as rows import

        int imported = 0;
        List<RowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2; // row 1 is the header
            try {
                ParsedRow row = parse(rows.get(i), byName);
                ActivityRequest req = row.request();
                RecordKey key = new RecordKey(req.facilityId(), req.category(), req.year(), req.month());
                if (!seen.add(key)) {
                    throw new RowException("duplicate: a record for " + row.facility().getName() + ", "
                            + req.category() + ", " + req.year() + "-" + String.format("%02d", req.month())
                            + " already exists");
                }
                try {
                    activities.createForImport(companyId, userId, row.facility(), req);
                } catch (RuntimeException e) {
                    seen.remove(key);   // the row did not save, so it must not block a later identical row
                    throw e;
                }
                imported++;
            } catch (RowException | BadRequestException | NotFoundException e) {
                errors.add(new RowError(rowNumber, e.getMessage()));
            } catch (DataIntegrityViolationException e) {
                errors.add(new RowError(rowNumber, "value is out of the allowed range"));
            }
        }
        if (imported > 0) {
            activities.ensureBaselineYear(companyId);   // once per upload, not once per row
        }
        return new UploadResult(imported, errors.size(), errors);
    }

    // ---------------------------------------------------------------- parsing

    private List<CSVRecord> readRows(InputStream in) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true).setTrim(true).setIgnoreEmptyLines(true)
                .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)   // two "year" columns would be ambiguous
                .build();
        try (Reader reader = withoutBom(in); CSVParser parser = format.parse(reader)) {
            checkHeader(parser.getHeaderMap().keySet());
            List<CSVRecord> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (rows.size() >= MAX_ROWS) {
                    throw new BadRequestException("CSV has more than " + MAX_ROWS + " rows. Split it into smaller files");
                }
                rows.add(record);
            }
            if (rows.isEmpty()) {
                throw new BadRequestException("CSV has a header but no data rows");
            }
            return rows;
        } catch (IOException | UncheckedIOException | IllegalArgumentException | IllegalStateException e) {
            throw new BadRequestException("Could not read the CSV file: " + rootMessage(e));
        }
    }

    private static void checkHeader(Set<String> headers) {
        if (headers.isEmpty()) {
            throw new BadRequestException("CSV file is empty");
        }
        Set<String> lower = headers.stream().map(h -> h.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<String> missing = REQUIRED_HEADERS.stream()
                .filter(h -> !lower.contains(h.toLowerCase(Locale.ROOT))).toList();
        if (!missing.isEmpty()) {
            throw new BadRequestException("CSV header must be " + String.join(",", REQUIRED_HEADERS)
                    + ". Missing: " + String.join(", ", missing));
        }
    }

    /** Excel saves UTF-8 CSV files with a byte order mark. Drop it so the first header still matches. */
    private static Reader withoutBom(InputStream in) throws IOException {
        PushbackReader reader = new PushbackReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1);
        int first = reader.read();
        if (first != -1 && first != '﻿') {
            reader.unread(first);
        }
        return reader;
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage() == null ? e.getClass().getSimpleName() : t.getMessage();
    }

    // ---------------------------------------------------------------- one row

    /** The facility comes from this company's own list, so it needs no second ownership check. */
    private record ParsedRow(Facility facility, ActivityRequest request) {
    }

    private ParsedRow parse(CSVRecord r, Map<String, List<Facility>> byName) {
        if (!r.isConsistent()) {
            throw new RowException("row has " + r.size() + " columns, expected " + r.getParser().getHeaderMap().size());
        }
        Facility facility = resolveFacility(r.get("facilityName"), byName);
        Long facilityId = facility.getId();
        Category category = parseCategory(r.get("category"));
        BigDecimal quantity = parseDecimal(r.get("quantity"));
        Integer year = parseInt("year", r.get("year"));
        Integer month = parseInt("month", r.get("month"));

        ActivityRequest req = new ActivityRequest(facilityId, category, quantity, year, month);
        Set<ConstraintViolation<ActivityRequest>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            throw new RowException(violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted().collect(Collectors.joining("; ")));
        }
        return new ParsedRow(facility, req);
    }

    private Map<String, List<Facility>> facilitiesByName(Long companyId) {
        Map<String, List<Facility>> map = new HashMap<>();
        for (Facility f : facilities.findByCompanyIdOrderByIdAsc(companyId)) {
            map.computeIfAbsent(normalize(f.getName()), k -> new ArrayList<>()).add(f);
        }
        return map;
    }

    private static Facility resolveFacility(String name, Map<String, List<Facility>> byName) {
        if (name.isBlank()) {
            throw new RowException("facilityName is required");
        }
        List<Facility> matches = byName.getOrDefault(normalize(name), List.of());
        if (matches.isEmpty()) {
            throw new RowException("facility '" + name + "' not found");
        }
        if (matches.size() > 1) {
            throw new RowException("facility name '" + name + "' matches more than one facility");
        }
        return matches.get(0);
    }

    private static Category parseCategory(String value) {
        try {
            return Category.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RowException("category must be one of " + Arrays.toString(Category.values()));
        }
    }

    private static BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new RowException("quantity must be a number");
        }
    }

    private static Integer parseInt(String field, String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new RowException(field + " must be a whole number");
        }
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    /** A problem with one row. Reported in the result, never thrown to the caller. */
    private static class RowException extends RuntimeException {
        RowException(String message) {
            super(message);
        }
    }
}
