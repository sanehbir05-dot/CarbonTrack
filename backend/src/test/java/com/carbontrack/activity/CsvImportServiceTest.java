package com.carbontrack.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.carbontrack.activity.dto.ActivityRequest;
import com.carbontrack.activity.dto.UploadResult;
import com.carbontrack.common.BadRequestException;
import com.carbontrack.common.Category;
import com.carbontrack.facility.Facility;
import com.carbontrack.facility.FacilityRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    private static final Long C = 42L;
    private static final Long U = 7L;
    private static final String HEADER = "facilityName,category,quantity,year,month\n";

    @Mock ActivityService activities;
    @Mock FacilityRepository facilities;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private CsvImportService service;

    private static Facility facility(long id, String name) {
        Facility f = new Facility();
        ReflectionTestUtils.setField(f, "id", id);
        f.setCompanyId(C);
        f.setName(name);
        return f;
    }

    @BeforeEach
    void setUp() {
        service = new CsvImportService(activities, facilities, validator);
    }

    private void facilitiesAre(Facility... fs) {
        when(facilities.findByCompanyIdOrderByIdAsc(C)).thenReturn(List.of(fs));
    }

    private UploadResult run(String csv) {
        return service.importCsv(C, U, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    private List<ActivityRequest> savedRequests(int times) {
        ArgumentCaptor<ActivityRequest> c = ArgumentCaptor.forClass(ActivityRequest.class);
        verify(activities, times(times)).createForImport(eq(C), eq(U), any(Facility.class), c.capture());
        return c.getAllValues();
    }

    @Test
    void importsValidRowsWithSourceCsv() {
        facilitiesAre(facility(1, "Pune Plant"), facility(2, "HQ"));

        UploadResult r = run(HEADER + "Pune Plant,ELECTRICITY,1000,2024,3\nHQ,DIESEL,25.5,2023,12\n");

        assertEquals(2, r.imported());
        assertEquals(0, r.failed());
        assertTrue(r.errors().isEmpty());
        List<ActivityRequest> saved = savedRequests(2);
        assertEquals(new ActivityRequest(1L, Category.ELECTRICITY, new BigDecimal("1000"), 2024, 3), saved.get(0));
        assertEquals(new ActivityRequest(2L, Category.DIESEL, new BigDecimal("25.5"), 2023, 12), saved.get(1));
    }

    @Test
    void passesTheMatchedFacilityThroughSoItIsNotLookedUpAgain() {
        Facility pune = facility(1, "Pune Plant");
        Facility hq = facility(2, "HQ");
        facilitiesAre(pune, hq);

        run(HEADER + "HQ,DIESEL,10,2024,1\n");

        ArgumentCaptor<Facility> f = ArgumentCaptor.forClass(Facility.class);
        verify(activities).createForImport(eq(C), eq(U), f.capture(), any(ActivityRequest.class));
        assertEquals(hq, f.getValue());
    }

    @Test
    void baselineYearIsEnsuredOncePerUploadNotPerRow() {
        facilitiesAre(facility(1, "Pune Plant"));

        run(HEADER + "Pune Plant,DIESEL,10,2024,1\nPune Plant,DIESEL,10,2024,2\nPune Plant,DIESEL,10,2024,3\n");

        verify(activities, times(1)).ensureBaselineYear(C);
    }

    @Test
    void baselineYearIsNotTouchedWhenNothingWasImported() {
        facilitiesAre(facility(1, "Pune Plant"));

        run(HEADER + "Nowhere,DIESEL,10,2024,1\n");

        verify(activities, never()).ensureBaselineYear(any());
    }

    @Test
    void matchesFacilityAndCategoryIgnoringCaseAndSpaces() {
        facilitiesAre(facility(1, "Pune Plant"));

        UploadResult r = run(HEADER + "  pune PLANT ,electricity , 10 , 2024 , 1\n");

        assertEquals(1, r.imported());
        assertEquals(Category.ELECTRICITY, savedRequests(1).get(0).category());
    }

    @Test
    void goodRowsAreSavedEvenWhenOthersFail_withSpreadsheetRowNumbers() {
        facilitiesAre(facility(1, "Pune Plant"));

        UploadResult r = run(HEADER
                + "Pune Plant,DIESEL,10,2024,1\n"         // row 2 ok
                + "Nowhere,DIESEL,10,2024,1\n"            // row 3 unknown facility
                + "Pune Plant,DIESEL,abc,2024,1\n"        // row 4 bad number
                + "Pune Plant,DIESEL,-5,2024,1\n"         // row 5 negative
                + "Pune Plant,DIESEL,10,2024,13\n"        // row 6 month
                + "Pune Plant,COAL,10,2024,1\n"           // row 7 category
                + "Pune Plant,DIESEL,10,20x4,1\n"         // row 8 year not a number
                + "Pune Plant,PETROL,7,2024,2\n");        // row 9 ok

        assertEquals(2, r.imported());
        assertEquals(6, r.failed());
        assertEquals(List.of(3, 4, 5, 6, 7, 8), r.errors().stream().map(UploadResult.RowError::row).toList());
        assertEquals("facility 'Nowhere' not found", r.errors().get(0).message());
        assertEquals("quantity must be a number", r.errors().get(1).message());
        assertEquals("quantity must be positive", r.errors().get(2).message());
        assertEquals("month must be between 1 and 12", r.errors().get(3).message());
        assertTrue(r.errors().get(4).message().startsWith("category must be one of [ELECTRICITY, DIESEL"));
        assertEquals("year must be a whole number", r.errors().get(5).message());
        savedRequests(2);
    }

    @Test
    void quantityWithMoreThanThreeDecimalsIsRejected() {
        facilitiesAre(facility(1, "Pune Plant"));

        UploadResult r = run(HEADER + "Pune Plant,DIESEL,1.2345,2024,1\n");

        assertEquals(1, r.failed());
        assertEquals("quantity must have at most 13 digits and 3 decimals", r.errors().get(0).message());
    }

    @Test
    void errorFromTheActivityServiceBecomesARowErrorAndOtherRowsContinue() {
        facilitiesAre(facility(1, "Pune Plant"));
        when(activities.createForImport(eq(C), eq(U), any(Facility.class), any(ActivityRequest.class)))
            .thenThrow(new BadRequestException("year must be between 2000 and 2026"))
            .thenReturn(null);

        UploadResult r = run(HEADER + "Pune Plant,DIESEL,10,2099,1\nPune Plant,DIESEL,10,2024,1\n");

        assertEquals(1, r.imported());
        assertEquals(1, r.failed());
        assertEquals(2, r.errors().get(0).row());
        assertEquals("year must be between 2000 and 2026", r.errors().get(0).message());
    }

    @Test
    void databaseRangeErrorBecomesARowError() {
        facilitiesAre(facility(1, "Pune Plant"));
        when(activities.createForImport(eq(C), eq(U), any(Facility.class), any(ActivityRequest.class)))
            .thenThrow(new DataIntegrityViolationException("numeric field overflow"));

        UploadResult r = run(HEADER + "Pune Plant,DIESEL,10,2024,1\n");

        assertEquals("value is out of the allowed range", r.errors().get(0).message());
    }

    @Test
    void ambiguousFacilityNameIsAnErrorNotAGuess() {
        facilitiesAre(facility(1, "Plant"), facility(2, "plant"));

        UploadResult r = run(HEADER + "Plant,DIESEL,10,2024,1\n");

        assertEquals(0, r.imported());
        assertEquals("facility name 'Plant' matches more than one facility", r.errors().get(0).message());
        verify(activities, never()).createForImport(any(), any(), any(), any());
    }

    @Test
    void otherCompaniesFacilitiesAreNeverVisible() {
        when(facilities.findByCompanyIdOrderByIdAsc(C)).thenReturn(List.of());   // only this company is queried

        UploadResult r = run(HEADER + "Pune Plant,DIESEL,10,2024,1\n");

        assertEquals("facility 'Pune Plant' not found", r.errors().get(0).message());
        verify(facilities).findByCompanyIdOrderByIdAsc(C);
        verifyNoMoreInteractions(facilities);
    }

    @Test
    void columnsMayBeInAnyOrderAndExtraColumnsAreIgnored() {
        facilitiesAre(facility(1, "Pune Plant"));

        UploadResult r = run("MONTH,notes,Year,quantity,CATEGORY,FacilityName\n3,hello,2024,50,diesel,Pune Plant\n");

        assertEquals(1, r.imported());
        assertEquals(new ActivityRequest(1L, Category.DIESEL, new BigDecimal("50"), 2024, 3), savedRequests(1).get(0));
    }

    @Test
    void utf8ByteOrderMarkFromExcelIsIgnored() {
        facilitiesAre(facility(1, "Pune Plant"));

        UploadResult r = run("﻿" + HEADER + "Pune Plant,DIESEL,10,2024,1\n");

        assertEquals(1, r.imported());
    }

    @Test
    void blankLinesAreSkippedAndQuotedCommasWork() {
        facilitiesAre(facility(1, "Plant, North"));

        UploadResult r = run(HEADER + "\n\"Plant, North\",DIESEL,10,2024,1\n\n");

        assertEquals(1, r.imported());
        assertEquals(0, r.failed());
    }

    @Test
    void rowWithTheWrongNumberOfColumnsIsAnError() {
        facilitiesAre(facility(1, "Pune Plant"));

        UploadResult r = run(HEADER + "Pune Plant,DIESEL,10,2024\n");

        assertEquals(1, r.failed());
        assertEquals("row has 4 columns, expected 5", r.errors().get(0).message());
    }

    @Test
    void missingHeaderColumnRejectsTheWholeFile() {
        BadRequestException e = assertThrows(BadRequestException.class,
                () -> run("facilityName,category,quantity,year\nPune Plant,DIESEL,10,2024\n"));

        assertTrue(e.getMessage().contains("Missing: month"));
        verify(activities, never()).createForImport(any(), any(), any(), any());
    }

    @Test
    void emptyFileAndHeaderOnlyFileAreRejected() {
        assertEquals("CSV file is empty",
                assertThrows(BadRequestException.class, () -> run("")).getMessage());
        assertEquals("CSV has a header but no data rows",
                assertThrows(BadRequestException.class, () -> run(HEADER)).getMessage());
    }

    @Test
    void duplicateHeaderNamesAreRejectedEvenWhenAllRequiredColumnsExist() {
        // All five required columns are present, but "year" appears twice, so which one counts is ambiguous.
        BadRequestException e = assertThrows(BadRequestException.class,
                () -> run("facilityName,category,quantity,year,month,Year\nPune Plant,DIESEL,10,2024,1,2025\n"));

        assertTrue(e.getMessage().startsWith("Could not read the CSV file"));
        verify(activities, never()).createForImport(any(), any(), any(), any());
    }

    @Test
    void unclosedQuoteIsRejected() {
        BadRequestException e = assertThrows(BadRequestException.class,
                () -> run(HEADER + "\"Pune Plant,DIESEL,10,2024,1\n"));

        assertTrue(e.getMessage().startsWith("Could not read the CSV file"));
        verify(activities, never()).createForImport(any(), any(), any(), any());
    }

    @Test
    void tooManyRowsAreRejectedBeforeAnythingIsSaved() {
        String rows = IntStream.range(0, CsvImportService.MAX_ROWS + 1)
                .mapToObj(i -> "Pune Plant,DIESEL,10,2024,1").collect(Collectors.joining("\n"));

        BadRequestException e = assertThrows(BadRequestException.class, () -> run(HEADER + rows + "\n"));

        assertTrue(e.getMessage().contains("more than " + CsvImportService.MAX_ROWS));
        verify(activities, never()).createForImport(any(), any(), any(), any());
    }

    @Test
    void exactlyTheMaximumNumberOfRowsIsAccepted() {
        facilitiesAre(facility(1, "Pune Plant"));
        String rows = IntStream.range(0, CsvImportService.MAX_ROWS)
                .mapToObj(i -> "Pune Plant,DIESEL,10,2024,1").collect(Collectors.joining("\n"));

        UploadResult r = run(HEADER + rows + "\n");

        assertEquals(CsvImportService.MAX_ROWS, r.imported());
        verify(activities, atLeastOnce()).createForImport(any(), any(), any(), any());
    }
}
