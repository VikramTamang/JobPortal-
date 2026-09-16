-- =========================================================
-- V1: Core schema for the multi-tenant job portal (MySQL 8)
-- =========================================================
-- UUIDs are generated app-side by Hibernate (@UuidGenerator) and stored
-- as CHAR(36), since MySQL has no native UUID type. No DEFAULT needed on
-- id columns -- the app always supplies the value before insert.

-- ---------------------------------------------------------
-- COMPANY  (the "tenant")
-- ---------------------------------------------------------
CREATE TABLE company (
                         id              CHAR(36) NOT NULL PRIMARY KEY,
                         name            VARCHAR(255) NOT NULL,
                         slug            VARCHAR(255) NOT NULL UNIQUE,
                         description     TEXT,
                         website         VARCHAR(500),
                         is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- APP_USER  (login identity for all three roles)
-- company_id is NULL for CANDIDATE and platform ADMIN;
-- required for RECRUITER.
-- ---------------------------------------------------------
CREATE TABLE app_user (
                          id              CHAR(36) NOT NULL PRIMARY KEY,
                          email           VARCHAR(255) NOT NULL UNIQUE,
                          password_hash   VARCHAR(255) NOT NULL,
                          role            VARCHAR(20)  NOT NULL,
                          company_id      CHAR(36),
                          is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                          is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                          created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                          CONSTRAINT chk_app_user_role CHECK (role IN ('CANDIDATE','RECRUITER','ADMIN')),
                          CONSTRAINT chk_recruiter_has_company
                              CHECK (role != 'RECRUITER' OR company_id IS NOT NULL),
    CONSTRAINT fk_app_user_company FOREIGN KEY (company_id) REFERENCES company(id)
) ENGINE=InnoDB;

CREATE INDEX idx_app_user_company ON app_user(company_id);
CREATE INDEX idx_app_user_role ON app_user(role);

-- ---------------------------------------------------------
-- CANDIDATE_PROFILE
-- skills is stored as JSON (MySQL has no array type)
-- ---------------------------------------------------------
CREATE TABLE candidate_profile (
                                   id                  CHAR(36) NOT NULL PRIMARY KEY,
                                   user_id             CHAR(36) NOT NULL UNIQUE,
                                   full_name           VARCHAR(255) NOT NULL,
                                   phone               VARCHAR(50),
                                   headline            VARCHAR(255),
                                   summary             TEXT,
                                   location            VARCHAR(255),
                                   experience_years    NUMERIC(4,1),
                                   skills              JSON,
                                   created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                   CONSTRAINT fk_candidate_profile_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- RECRUITER_PROFILE
-- ---------------------------------------------------------
CREATE TABLE recruiter_profile (
                                   id              CHAR(36) NOT NULL PRIMARY KEY,
                                   user_id         CHAR(36) NOT NULL UNIQUE,
                                   company_id      CHAR(36) NOT NULL,
                                   full_name       VARCHAR(255) NOT NULL,
                                   job_title       VARCHAR(255),
                                   department      VARCHAR(255),
                                   created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                   CONSTRAINT fk_recruiter_profile_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
                                   CONSTRAINT fk_recruiter_profile_company FOREIGN KEY (company_id) REFERENCES company(id)
) ENGINE=InnoDB;

CREATE INDEX idx_recruiter_profile_company ON recruiter_profile(company_id);

-- ---------------------------------------------------------
-- JOB
-- ---------------------------------------------------------
CREATE TABLE job (
                     id                      CHAR(36) NOT NULL PRIMARY KEY,
                     company_id              CHAR(36) NOT NULL,
                     created_by_recruiter_id CHAR(36) NOT NULL,

                     title                   VARCHAR(255) NOT NULL,
                     description             TEXT NOT NULL,
                     requirements            TEXT,
                     responsibilities        TEXT,

                     employment_type         VARCHAR(20) NOT NULL,
                     experience_level        VARCHAR(20) NOT NULL,
                     work_mode               VARCHAR(20) NOT NULL,

                     location                VARCHAR(255),
                     salary_min              NUMERIC(12,2),
                     salary_max              NUMERIC(12,2),
                     currency                VARCHAR(10) DEFAULT 'USD',

                     vacancies               INT NOT NULL DEFAULT 1,
                     application_deadline    DATE,

                     status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

                     published_at            DATETIME(6),
                     created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                     updated_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                     CONSTRAINT chk_job_employment_type CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','TEMPORARY')),
                     CONSTRAINT chk_job_experience_level CHECK (experience_level IN ('ENTRY','JUNIOR','MID','SENIOR','LEAD','EXECUTIVE')),
                     CONSTRAINT chk_job_work_mode CHECK (work_mode IN ('REMOTE','HYBRID','ON_SITE')),
                     CONSTRAINT chk_job_status CHECK (status IN ('DRAFT','PUBLISHED','CLOSED','ARCHIVED')),
                     CONSTRAINT chk_job_vacancies CHECK (vacancies > 0),
                     CONSTRAINT chk_salary_range CHECK (salary_max IS NULL OR salary_min IS NULL OR salary_max >= salary_min),

                     CONSTRAINT fk_job_company FOREIGN KEY (company_id) REFERENCES company(id),
                     CONSTRAINT fk_job_recruiter FOREIGN KEY (created_by_recruiter_id) REFERENCES recruiter_profile(id)
) ENGINE=InnoDB;

CREATE INDEX idx_job_company ON job(company_id);
CREATE INDEX idx_job_status ON job(status);
CREATE INDEX idx_job_company_status ON job(company_id, status);
CREATE INDEX idx_job_employment_type ON job(employment_type);
CREATE INDEX idx_job_experience_level ON job(experience_level);
CREATE INDEX idx_job_work_mode ON job(work_mode);
CREATE INDEX idx_job_location ON job(location);
CREATE INDEX idx_job_deadline ON job(application_deadline);
CREATE INDEX idx_job_published_at ON job(published_at);

-- MySQL's equivalent of Postgres's GIN full-text index. Query later with
-- MATCH(title, description) AGAINST('...' IN NATURAL LANGUAGE MODE).
CREATE FULLTEXT INDEX idx_job_search_text ON job(title, description);

-- ---------------------------------------------------------
-- JOB_SKILL  (normalized, enables filtering by required skill)
-- ---------------------------------------------------------
CREATE TABLE job_skill (
                           id          CHAR(36) NOT NULL PRIMARY KEY,
                           job_id      CHAR(36) NOT NULL,
                           skill_name  VARCHAR(100) NOT NULL,

                           CONSTRAINT fk_job_skill_job FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_job_skill_job ON job_skill(job_id);
CREATE INDEX idx_job_skill_name ON job_skill(skill_name);

-- ---------------------------------------------------------
-- RESUME
-- ---------------------------------------------------------
CREATE TABLE resume (
                        id              CHAR(36) NOT NULL PRIMARY KEY,
                        candidate_id    CHAR(36) NOT NULL,
                        file_name       VARCHAR(255) NOT NULL,
                        storage_key     VARCHAR(500) NOT NULL UNIQUE,
                        content_type    VARCHAR(100) NOT NULL,
                        file_size_bytes BIGINT NOT NULL,
                        is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
                        uploaded_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                        CONSTRAINT fk_resume_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_profile(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_resume_candidate ON resume(candidate_id);

-- ---------------------------------------------------------
-- APPLICATION
-- company_id is denormalized here (copied from job.company_id)
-- specifically so tenant-scoped queries never need a join to
-- enforce isolation -- every recruiter-facing query can filter
-- directly on application.company_id.
-- ---------------------------------------------------------
CREATE TABLE application (
                             id              CHAR(36) NOT NULL PRIMARY KEY,
                             job_id          CHAR(36) NOT NULL,
                             company_id      CHAR(36) NOT NULL,
                             candidate_id    CHAR(36) NOT NULL,
                             resume_id       CHAR(36),

                             cover_letter    TEXT,
                             status          VARCHAR(20) NOT NULL DEFAULT 'APPLIED',

                             applied_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                             updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                             CONSTRAINT uq_application_job_candidate UNIQUE (job_id, candidate_id),
                             CONSTRAINT chk_application_status CHECK (status IN ('APPLIED','SCREENING','SHORTLISTED','INTERVIEW','OFFER','HIRED','REJECTED','WITHDRAWN')),

                             CONSTRAINT fk_application_job FOREIGN KEY (job_id) REFERENCES job(id),
                             CONSTRAINT fk_application_company FOREIGN KEY (company_id) REFERENCES company(id),
                             CONSTRAINT fk_application_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_profile(id),
                             CONSTRAINT fk_application_resume FOREIGN KEY (resume_id) REFERENCES resume(id)
) ENGINE=InnoDB;

CREATE INDEX idx_application_company ON application(company_id);
CREATE INDEX idx_application_job ON application(job_id);
CREATE INDEX idx_application_candidate ON application(candidate_id);
CREATE INDEX idx_application_status ON application(status);
CREATE INDEX idx_application_company_status ON application(company_id, status);

-- ---------------------------------------------------------
-- APPLICATION_STATUS_HISTORY  (audit trail of the workflow)
-- ---------------------------------------------------------
CREATE TABLE application_status_history (
                                            id              CHAR(36) NOT NULL PRIMARY KEY,
                                            application_id  CHAR(36) NOT NULL,
                                            previous_status VARCHAR(20),
                                            new_status      VARCHAR(20) NOT NULL,
                                            changed_by      CHAR(36) NOT NULL,
                                            note            TEXT,
                                            changed_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                            CONSTRAINT fk_ash_application FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE CASCADE,
                                            CONSTRAINT fk_ash_changed_by FOREIGN KEY (changed_by) REFERENCES app_user(id)
) ENGINE=InnoDB;

CREATE INDEX idx_app_status_history_application ON application_status_history(application_id);

-- ---------------------------------------------------------
-- INTERVIEW
-- ---------------------------------------------------------
CREATE TABLE interview (
                           id                  CHAR(36) NOT NULL PRIMARY KEY,
                           application_id      CHAR(36) NOT NULL,
                           scheduled_at        DATETIME(6) NOT NULL,
                           mode                VARCHAR(20) NOT NULL,
                           location_or_link    VARCHAR(500),
                           interviewer_notes   TEXT,
                           status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
                           created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                           updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                           CONSTRAINT chk_interview_mode CHECK (mode IN ('ONLINE','PHONE','ON_SITE')),
                           CONSTRAINT chk_interview_status CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED','RESCHEDULED')),
                           CONSTRAINT fk_interview_application FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_interview_application ON interview(application_id);

-- ---------------------------------------------------------
-- NOTIFICATION  (email/queue-ready outbox)
-- ---------------------------------------------------------
CREATE TABLE notification (
                              id                  CHAR(36) NOT NULL PRIMARY KEY,
                              recipient_user_id   CHAR(36) NOT NULL,
                              type                VARCHAR(50) NOT NULL,
                              subject             VARCHAR(255) NOT NULL,
                              body                TEXT NOT NULL,
                              status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                              related_entity_type VARCHAR(50),
                              related_entity_id   CHAR(36),
                              created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              sent_at             DATETIME(6),

                              CONSTRAINT chk_notification_status CHECK (status IN ('PENDING','SENT','FAILED')),
                              CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES app_user(id)
) ENGINE=InnoDB;

CREATE INDEX idx_notification_recipient ON notification(recipient_user_id);
CREATE INDEX idx_notification_status ON notification(status);

-- ---------------------------------------------------------
-- AUDIT_LOG  (platform-wide activity trail)
-- company_id nullable: platform-level admin actions have none.
-- metadata uses MySQL's native JSON type.
-- ---------------------------------------------------------
CREATE TABLE audit_log (
                           id              CHAR(36) NOT NULL PRIMARY KEY,
                           company_id      CHAR(36),
                           actor_user_id   CHAR(36),
                           action          VARCHAR(100) NOT NULL,
                           entity_type     VARCHAR(100) NOT NULL,
                           entity_id       CHAR(36),
                           metadata        JSON,
                           ip_address      VARCHAR(64),
                           created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                           CONSTRAINT fk_audit_log_company FOREIGN KEY (company_id) REFERENCES company(id),
                           CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES app_user(id)
) ENGINE=InnoDB;

CREATE INDEX idx_audit_log_company ON audit_log(company_id);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_user_id);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);

-- ---------------------------------------------------------
-- REFRESH_TOKEN
-- ---------------------------------------------------------
CREATE TABLE refresh_token (
                               id              CHAR(36) NOT NULL PRIMARY KEY,
                               user_id         CHAR(36) NOT NULL,
                               token_hash      VARCHAR(255) NOT NULL UNIQUE,
                               expires_at      DATETIME(6) NOT NULL,
                               revoked         BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                               CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);