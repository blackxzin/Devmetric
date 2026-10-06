-- Perfil publico opcional: /u/{username}. Desligado por padrao (privacidade).

ALTER TABLE users ADD COLUMN username VARCHAR(40);
ALTER TABLE users ADD COLUMN public_profile BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX uk_users_username ON users (LOWER(username));
