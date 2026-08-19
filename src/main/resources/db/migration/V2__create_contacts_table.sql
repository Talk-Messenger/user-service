CREATE TABLE contacts (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (user_id, contact_user_id),
    CHECK ( user_id != contact_user_id )
);

CREATE INDEX ix_contacts_user_id ON contacts(user_id);
CREATE INDEX ix_contacts_contact_user_id ON contacts(contact_user_id);