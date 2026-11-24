package com.carbontrack.activity;

import com.carbontrack.activity.dto.ActivityDto;
import com.carbontrack.activity.dto.ActivityRequest;
import com.carbontrack.calculation.CalculationResult;
import com.carbontrack.calculation.EmissionCalculator;
import com.carbontrack.common.BadRequestException;
import com.carbontrack.common.Category;
import com.carbontrack.common.NotFoundException;
import com.carbontrack.common.PageResponse;
import com.carbontrack.common.RecordSource;
import com.carbontrack.company.Company;
import com.carbontrack.company.CompanyRepository;
import com.carbontrack.facility.Facility;
import com.carbontrack.facility.FacilityService;
import java.time.Year;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every method takes the caller's companyId. It never comes from the request body. */
@Service
public class ActivityService {

    static final int MIN_YEAR = 2000;
    static final int MAX_PAGE_SIZE = 100;
    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("year"), Sort.Order.desc("month"), Sort.Order.desc("id"));

    private final ActivityRecordRepository records;
    private final FacilityService facilities;
    private final EmissionCalculator calculator;
    private final CompanyRepository companies;

    public ActivityService(ActivityRecordRepository records, FacilityService facilities,
                           EmissionCalculator calculator, CompanyRepository companies) {
        this.records = records;
        this.facilities = facilities;
        this.calculator = calculator;
        this.companies = companies;
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityDto> list(Long companyId, Long facilityId, Category category, Integer year,
                                          int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page must be 0 or more");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        Specification<ActivityRecord> spec = (root, q, cb) -> cb.equal(root.get("companyId"), companyId);
        if (facilityId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("facility").get("id"), facilityId));
        }
        if (category != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category"), category));
        }
        if (year != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("year"), year));
        }
        Page<ActivityRecord> result = records.findAll(spec, PageRequest.of(page, size, NEWEST_FIRST));
        return PageResponse.from(result, ActivityDto::from);
    }

    @Transactional
    public ActivityDto create(Long companyId, Long userId, ActivityRequest req) {
        return create(companyId, userId, req, RecordSource.MANUAL);
    }

    /** Same as a manual create, but records where the row came from (for example CSV). */
    @Transactional
    public ActivityDto create(Long companyId, Long userId, ActivityRequest req, RecordSource source) {
        ActivityRecord r = new ActivityRecord();
        r.setCompanyId(companyId);
        r.setSource(source);
        r.setCreatedBy(userId);
        fill(r, companyId, req);
        records.saveAndFlush(r);
        ensureBaselineYear(companyId);
        return ActivityDto.from(r);
    }

    /**
     * Fast path for CSV import. The caller has already taken {@code facility} from the company's own
     * facility list, so it is not looked up again, and it calls {@link #ensureBaselineYear} once at the end
     * instead of after every row. Validation and calculation are the same as for a manual record.
     */
    @Transactional
    public ActivityDto createForImport(Long companyId, Long userId, Facility facility, ActivityRequest req) {
        ActivityRecord r = new ActivityRecord();
        r.setCompanyId(companyId);
        r.setSource(RecordSource.CSV);
        r.setCreatedBy(userId);
        fillWith(r, facility, req);
        records.saveAndFlush(r);
        return ActivityDto.from(r);
    }

    @Transactional
    public ActivityDto update(Long companyId, Long id, ActivityRequest req) {
        ActivityRecord r = find(companyId, id);
        fill(r, companyId, req);
        records.saveAndFlush(r);
        ensureBaselineYear(companyId);
        return ActivityDto.from(r);
    }

    @Transactional
    public void delete(Long companyId, Long id) {
        records.delete(find(companyId, id));
    }

    private ActivityRecord find(Long companyId, Long id) {
        return records.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Activity record not found"));
    }

    /** Validates, derives unit and scope from the category, and (re)calculates the emission. */
    private void fill(ActivityRecord r, Long companyId, ActivityRequest req) {
        validateYear(req.year());   // checked before the facility, so the error order is stable
        fillWith(r, facilities.getOwned(companyId, req.facilityId()), req);
    }

    private static void validateYear(int year) {
        int maxYear = Year.now().getValue();
        if (year < MIN_YEAR || year > maxYear) {
            throw new BadRequestException("year must be between " + MIN_YEAR + " and " + maxYear);
        }
    }

    private void fillWith(ActivityRecord r, Facility facility, ActivityRequest req) {
        validateYear(req.year());
        CalculationResult calc = calculator.calculate(req.category(), req.quantity(), req.year());

        r.setFacility(facility);
        r.setCategory(req.category());
        r.setScope(req.category().getScope());
        r.setUnit(req.category().getUnit());
        r.setQuantity(req.quantity());
        r.setYear(req.year());
        r.setMonth(req.month());
        r.setFactor(calc.factor());
        r.setCo2eKg(calc.co2eKg());
    }

    /** Keys of the company's existing records, for CSV duplicate detection. */
    @Transactional(readOnly = true)
    public Set<RecordKey> existingKeys(Long companyId) {
        return new HashSet<>(records.findAllKeys(companyId));
    }

    /** If the company has no baseline year yet, use the earliest year that has data. */
    @Transactional
    public void ensureBaselineYear(Long companyId) {
        Company company = companies.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        if (company.getBaselineYear() == null) {
            Integer earliest = records.findMinYear(companyId);
            if (earliest != null) {
                company.setBaselineYear(earliest);
            }
        }
    }
}
