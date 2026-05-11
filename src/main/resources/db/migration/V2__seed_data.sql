-- Day 26: Seed initial data - admin user
-- Password: admin123 (BCrypt hash)

INSERT INTO users (username, password, role, created_at)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', NOW())
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password, role, created_at)
VALUES ('testuser', '$2a$10$slYQmyNdGzin7olVN3p5be07IvcWZLEBU/msHqFp5PLKY/xQK2MDi', 'USER', NOW())
ON CONFLICT (username) DO NOTHING;
