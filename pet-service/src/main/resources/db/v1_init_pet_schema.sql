CREATE TABLE pet_ads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id       UUID NOT NULL,

    ad_type         VARCHAR(20)  NOT NULL
                    CHECK (ad_type IN ('LOST', 'FOUND', 'SIGHTING')),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'RESOLVED', 'ARCHIVED', 'EXPIRED')),

    species         VARCHAR(30)  NOT NULL
                    CHECK (species IN ('DOG', 'CAT', 'OTHER')),
    breed           VARCHAR(100),
    color           VARCHAR(100) NOT NULL,
    pet_name        VARCHAR(100),
    description     TEXT,

    event_date      DATE         NOT NULL,
    location        geography(Point, 4326) NOT NULL,
    address_text    VARCHAR(500),

    contact_phone    VARCHAR(20),
    contact_email    VARCHAR(255)   NOT NULL,
    contact_telegram VARCHAR(100),

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ

);

CREATE INDEX idx_pet_ads_location       ON pet_ads USING GIST(location);
CREATE INDEX idx_pet_ads_status_created ON pet_ads(status, created_at DESC);
CREATE INDEX idx_pet_ads_type_status    ON pet_ads(ad_type, status);
CREATE INDEX idx_pet_ads_species        ON pet_ads(species);
CREATE INDEX idx_pet_ads_author         ON pet_ads(author_id);

CREATE TABLE pet_photos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pet_ad_id       UUID         NOT NULL REFERENCES pet_ads(id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    thumbnail_url   VARCHAR(500),
    sort_order      INT          NOT NULL DEFAULT 0
                                 CHECK (sort_order >= 0 AND sort_order < 10),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_pet_photos_order UNIQUE (pet_ad_id, sort_order)
);



