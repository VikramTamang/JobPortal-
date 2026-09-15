-- Demo tenant for testing recruiter registration and job posting before
-- the Admin company-management endpoints exist (later phase).
INSERT INTO company (id, name, slug, description, is_active)
VALUES (
           uuid_generate_v4(),
           'Acme Corp',
           'acme-corp',
           'Demo company seeded for local development and testing.',
           TRUE
       );