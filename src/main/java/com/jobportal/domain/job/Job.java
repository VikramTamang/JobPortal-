package com.jobportal.domain.job;

import com.jobportal.domain.common.BaseEntity;
import com.jobportal.domain.company.Company;
import com.jobportal.domain.user.RecruiterProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job")
public class Job extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_recruiter_id", nullable = false)
    private RecruiterProfile createdByRecruiter;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 20)
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 20)
    private WorkMode workMode;

    private String location;

    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;

    private String currency = "USD";

    @Column(nullable = false)
    private int vacancies = 1;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @BatchSize(size = 25)
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobSkill> skills = new ArrayList<>();

    public void addSkill(String skillName) {
        JobSkill skill = new JobSkill();
        skill.setJob(this);
        skill.setSkillName(skillName);
        this.skills.add(skill);
    }

    /**
     * orphanRemoval=true on the collection means clearing it and re-adding
     * deletes the old JobSkill rows and inserts fresh ones — simpler and
     * safer than diffing old vs new skill lists on every update.
     */
    public void replaceSkills(List<String> skillNames) {
        this.skills.clear();
        if (skillNames != null) {
            skillNames.forEach(this::addSkill);
        }
    }
}