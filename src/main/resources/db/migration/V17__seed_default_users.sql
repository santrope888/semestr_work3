INSERT INTO users (
    username, email, password, phone_number,
    created_at, role_id,
    license_status, passport_status
)
VALUES (
           'admin',
           'admin@auramotum.ru',
           '$2a$10$VFGSLDPxPhlpho82xEUB4OlERYK2vslx5s.eOkq8LSmTCG8KIKkaW',
           '+7 (900) 000-00-01',
           CURRENT_DATE,
           (SELECT role_id FROM roles WHERE name = 'ADMIN'),
           'NOT_UPLOADED',
           'NOT_UPLOADED'
       )
    ON CONFLICT (username) DO NOTHING;

INSERT INTO users (
    username, email, password, phone_number,
    created_at, role_id,
    license_status, passport_status
)
VALUES (
           'user',
           'user@auramotum.ru',
           '$2a$10$lZmZyeeU3ow9bG4szAlHFutF1/RKacfh.TYoQL5219HsyctAJDBq2',
           '+7 (900) 000-00-02',
           CURRENT_DATE,
           (SELECT role_id FROM roles WHERE name = 'USER'),
           'NOT_UPLOADED',
           'NOT_UPLOADED'
       )
    ON CONFLICT (username) DO NOTHING;