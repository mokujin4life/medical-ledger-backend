DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

CREATE TABLE IF NOT EXISTS address
(
  id                  SERIAL       NOT NULL
    CONSTRAINT address_pkey
    PRIMARY KEY,
  administrative_area VARCHAR(255) NOT NULL,
  street              VARCHAR(255) NOT NULL,
  street_number       VARCHAR(255) NOT NULL,
  appartment_number   VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS national_passport
(
  id             SERIAL       NOT NULL
    CONSTRAINT national_passport_pkey
    PRIMARY KEY,
  first_name     VARCHAR(255) NOT NULL,
  last_name      VARCHAR(255) NOT NULL,
  father_name    VARCHAR(255) NOT NULL,
  date_of_birth  DATE         NOT NULL,
  place_of_birth VARCHAR(255) NOT NULL,
  image_path     VARCHAR(255),
  sex            VARCHAR(255) NOT NULL,
  issuer         VARCHAR(255) NOT NULL,
  date_of_issue  DATE         NOT NULL
);

CREATE TABLE IF NOT EXISTS place_of_residence
(
  id                   SERIAL  NOT NULL
    CONSTRAINT place_of_residence_pkey
    PRIMARY KEY,
  address_id           INTEGER NOT NULL
    CONSTRAINT fk_por_address
    REFERENCES address,
  start_date           DATE    NOT NULL,
  end_date             DATE,
  national_passport_id INTEGER
    CONSTRAINT fk_por_national_passport
    REFERENCES national_passport
);

CREATE TABLE IF NOT EXISTS national_number
(
  number            VARCHAR(255) NOT NULL UNIQUE
    CONSTRAINT national_number_pkey
    PRIMARY KEY,
  registration_date DATE         NOT NULL,
  issuer            VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS known_identity
(
  id                   SERIAL       NOT NULL
    CONSTRAINT known_identity_pkey
    PRIMARY KEY,
  national_passport_id INTEGER      NOT NULL
    CONSTRAINT fk_ki_national_passport
    REFERENCES national_passport,
  national_number      VARCHAR(255) NOT NULL
    CONSTRAINT fk_ki_national_number
    REFERENCES national_number
);
