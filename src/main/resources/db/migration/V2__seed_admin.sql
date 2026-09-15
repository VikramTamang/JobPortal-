-- Seed a platform admin so there's a way into the system before any
-- company/recruiter exists. Password below is bcrypt("Admin@12345").
-- CHANGE THIS PASSWORD immediately in any real deployment.

INSERT INTO app_user (id, email, password_hash, role, is_active, is_email_verified)
VALUES (
           uuid_generate_v4(),
           'admin@jobportal.com',
           '$2b$10$ddMsBDObQ3zQtcNxY1jhyOwm3H2SyKgtYaTSg.KRCILJAhgr4PIQ2',
           'ADMIN',
           TRUE,
           TRUE
       );