package com.carbontrack.activity;

import com.carbontrack.common.Category;

/**
 * What makes two activity records "the same" for CSV duplicate detection:
 * same facility, category, year and month. Quantity is deliberately not part of it,
 * so a corrected re-upload of the same period is still flagged instead of doubling the emissions.
 */
public record RecordKey(Long facilityId, Category category, Integer year, Integer month) {
}
