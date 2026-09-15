-- =========================================================
-- V1: Core schema for the multi-tenant job portal
-- =========================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------
-- COMPANY  (the "tenant")
-- ---------------------------------------------------------
CREATE TABLE company (
                         id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         name            VARCHAR(255) NOT NULL,
                         slug            VARCHAR(255) NOT NULL UNIQUE,
                         description     TEXT,
                         website         VARCHAR(500),
                         is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                         updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------
-- APP_USER  (login identity for all three roles)
-- company_id is NULL for CANDIDATE and platform ADMIN;
-- required for RECRUITER.
-- ---------------------------------------------------------
CREATE TABLE app_user (
                          id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          email           VARCHAR(255) NOT NULL UNIQUE,
                          password_hash   VARCHAR(255) NOT NULL,
                          role            VARCHAR(20)  NOT NULL CHECK (role IN ('CANDIDATE','RECRUITER','ADMIN')),
                          company_id      UUID REFERENCES company(id),
                          is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                          is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                          updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

                          CONSTRAINT chk_recruiter_has_company
                              CHECK (role != 'RECRUITER' OR company_id IS NOT NULL)
    );

CREATE INDEX idx_app_user_company ON app_user(company_id);
CREATE INDEX idx_app_user_role ON app_user(role);

-- ---------------------------------------------------------
-- CANDIDATE_PROFILE
-- ---------------------------------------------------------
CREATE TABLE candidate_profile (
                                   id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                   user_id             UUID NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
                                   full_name           VARCHAR(255) NOT NULL,
                                   phone               VARCHAR(50),
                                   headline            VARCHAR(255),
                                   summary             TEXT,
                                   location             VARCHAR(255),
                                   experience_years    NUMERIC(4,1),
                                   skills              TEXT[],
                                   created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------
-- RECRUITER_PROFILE
-- ---------------------------------------------------------
CREATE TABLE recruiter_profile (
                                   id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                   user_id         UUID NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
                                   company_id      UUID NOT NULL REFERENCES company(id),
                                   full_name       VARCHAR(255) NOT NULL,
                                   job_title       VARCHAR(255),
                                   department      VARCHAR(255),
                                   created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recruiter_profile_company ON recruiter_profile(company_id);

-- ---------------------------------------------------------
-- JOB
-- ---------------------------------------------------------
CREATE TABLE job (
                     id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                     company_id              UUID NOT NULL REFERENCES company(id),
                     created_by_recruiter_id UUID NOT NULL REFERENCES recruiter_profile(id),

                     title                   VARCHAR(255) NOT NULL,
                     description             TEXT NOT NULL,
                     requirements            TEXT,
                     responsibilities        TEXT,

                     employment_type         VARCHAR(20) NOT NULL
                         CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','TEMPORARY')),
                     experience_level        VARCHAR(20) NOT NULL
                         CHECK (experience_level IN ('ENTRY','JUNIOR','MID','SENIOR','LEAD','EXECUTIVE')),
                     work_mode               VARCHAR(20) NOT NULL
                         CHECK (work_mode IN ('REMOTE','HYBRID','ON_SITE')),

                     location                VARCHAR(255),
                     salary_min              NUMERIC(12,2),
                     salary_max              NUMERIC(12,2),
                     currency                VARCHAR(10) DEFAULT 'USD',

                     vacancies               INT NOT NULL DEFAULT 1 CHECK (vacancies > 0),
                     application_deadline    DATE,

                     status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                         CHECK (status IN ('DRAFT','PUBLISHED','CLOSED','ARCHIVED')),

                     published_at            TIMESTAMPTZ,
                     created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
                     updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

                     CONSTRAINT chk_salary_range CHECK (salary_max IS NULL OR salary_min IS NULL OR salary_max >= salary_min)
);

-- Search & tenant-scoping indexes
CREATE INDEX idx_job_company ON job(company_id);
CREATE INDEX idx_job_status ON job(status);
CREATE INDEX idx_job_company_status ON job(company_id, status);
CREATE INDEX idx_job_employment_type ON job(employment_type);
CREATE INDEX idx_job_experience_level ON job(experience_level);
CREATE INDEX idx_job_work_mode ON job(work_mode);
CREATE INDEX idx_job_location ON job(location);
CREATE INDEX idx_job_deadline ON job(application_deadline);
CREATE INDEX idx_job_published_at ON job(published_at);
-- full-text search on title + description
CREATE INDEX idx_job_search_text ON job USING GIN (to_tsvector('english', title || ' ' || description));

-- ---------------------------------------------------------
-- JOB_SKILL  (normalized, enables filtering by required skill)
-- ---------------------------------------------------------
CREATE TABLE job_skill (
                           id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           job_id      UUID NOT NULL REFERENCES job(id) ON DELETE CASCADE,
                           skill_name  VARCHAR(100) NOT NULL
);

CREATE INDEX idx_job_skill_job ON job_skill(job_id);
CREATE INDEX idx_job_skill_name ON job_skill(skill_name);

-- ---------------------------------------------------------
-- RESUME
-- ---------------------------------------------------------
CREATE TABLE resume (
                        id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        candidate_id    UUID NOT NULL REFERENCES candidate_profile(id) ON DELETE CASCADE,
                        file_name       VARCHAR(255) NOT NULL,
                        storage_key     VARCHAR(500) NOT NULL UNIQUE,
                        content_type    VARCHAR(100) NOT NULL,
                        file_size_bytes BIGINT NOT NULL,
                        is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
                        uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_resume_candidate ON resume(candidate_id);

-- ---------------------------------------------------------
-- APPLICATION
-- company_id is denormalized here (copied from job.company_id)
-- specifically so tenant-scoped queries never need a join to
-- enforce isolation -- every recruiter-facing query can filter
-- directly on application.company_id.
-- ---------------------------------------------------------
CREATE TABLE application (
                             id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             job_id          UUID NOT NULL REFERENCES job(id),
                             company_id      UUID NOT NULL REFERENCES company(id),
                             candidate_id    UUID NOT NULL REFERENCES candidate_profile(id),
                             resume_id       UUID REFERENCES resume(id),

                             cover_letter    TEXT,
                             status          VARCHAR(20) NOT NULL DEFAULT 'APPLIED'
                                 CHECK (status IN ('APPLIED','SCREENING','SHORTLISTED','INTERVIEW','OFFER','HIRED','REJECTED','WITHDRAWN')),

                             applied_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                             updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- a candidate can only apply once per job
                             CONSTRAINT uq_application_job_candidate UNIQUE (job_id, candidate_id)
);

CREATE INDEX idx_application_company ON application(company_id);
CREATE INDEX idx_application_job ON application(job_id);
CREATE INDEX idx_application_candidate ON application(candidate_id);
CREATE INDEX idx_application_status ON application(status);
CREATE INDEX idx_application_company_status ON application(company_id, status);

-- ---------------------------------------------------------
-- APPLICATION_STATUS_HISTORY  (audit trail of the workflow)
-- ---------------------------------------------------------
CREATE TABLE application_status_history (
                                            id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                            application_id  UUID NOT NULL REFERENCES application(id) ON DELETE CASCADE,
                                            previous_status VARCHAR(20),
                                            new_status      VARCHAR(20) NOT NULL,
                                            changed_by      UUID NOT NULL REFERENCES app_user(id),
                                            note            TEXT,
                                            changed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_app_status_history_application ON application_status_history(application_id);

-- ---------------------------------------------------------
-- INTERVIEW
-- ---------------------------------------------------------
CREATE TABLE interview (
                           id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           application_id  UUID NOT NULL REFERENCES application(id) ON DELETE CASCADE,
                           scheduled_at    TIMESTAMPTZ NOT NULL,
                           mode            VARCHAR(20) NOT NULL CHECK (mode IN ('ONLINE','PHONE','ON_SITE')),
                           location_or_link VARCHAR(500),
                           interviewer_notes TEXT,
                           status          VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                               CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED','RESCHEDULED')),
                           created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                           updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_interview_application ON interview(application_id);

-- ---------------------------------------------------------
-- NOTIFICATION  (email/queue-ready outbox)
-- ---------------------------------------------------------
CREATE TABLE notification (
                              id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              recipient_user_id UUID NOT NULL REFERENCES app_user(id),
                              type            VARCHAR(50) NOT NULL,
                              subject         VARCHAR(255) NOT NULL,
                              body            TEXT NOT NULL,
                              status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING','SENT','FAILED')),
                              related_entity_type VARCHAR(50),
                              related_entity_id   UUID,
                              created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                              sent_at         TIMESTAMPTZ
);

CREATE INDEX idx_notification_recipient ON notification(recipient_user_id);
CREATE INDEX idx_notification_status ON notification(status);

-- ---------------------------------------------------------
-- AUDIT_LOG  (platform-wide activity trail)
-- company_id nullable: platform-level admin actions have none.
-- ---------------------------------------------------------
CREATE TABLE audit_log (
                           id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           company_id      UUID REFERENCES company(id),
                           actor_user_id   UUID REFERENCES app_user(id),
                           action          VARCHAR(100) NOT NULL,
                           entity_type     VARCHAR(100) NOT NULL,
                           entity_id       UUID,
                           metadata        JSONB,
                           ip_address      VARCHAR(64),
                           created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_company ON audit_log(company_id);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_user_id);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);

-- ---------------------------------------------------------
-- REFRESH_TOKEN  (for JWT refresh flow, Phase 3)
-- ---------------------------------------------------------
CREATE TABLE refresh_token (
                               id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                               token_hash      VARCHAR(255) NOT NULL UNIQUE,
                               expires_at      TIMESTAMPTZ NOT NULL,
                               revoked         BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);