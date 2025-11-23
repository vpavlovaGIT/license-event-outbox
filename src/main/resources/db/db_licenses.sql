CREATE TABLE IF NOT EXISTS outbox_event (
                                            id SERIAL PRIMARY KEY,
                                            payload TEXT NOT NULL,
                                            processed BOOLEAN DEFAULT FALSE,
                                            created_at TIMESTAMP NOT NULL,
                                            processed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS software_license (
                                                id SERIAL PRIMARY KEY,
                                                license_id VARCHAR(255) UNIQUE NOT NULL,
    software_name VARCHAR(255),
    owner VARCHAR(255),
    expires_at DATE
    );