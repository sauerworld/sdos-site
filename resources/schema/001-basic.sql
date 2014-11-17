CREATE TABLE articles (
       id             serial,
       created_date   timestamp,
       published_date timestamp,
       published      boolean,
       title          text,
       author         text,
       category       text,
       content        text,
       PRIMARY KEY(id)
);

CREATE INDEX ON articles (category);

CREATE TABLE users (
       id             serial,
       username       text,
       password       text,
       email          text,
       validation_key text,
       validated      boolean,
       pubkey         text,
       created_date   timestamp,
       admin          boolean,
       PRIMARY KEY(id)
);

CREATE INDEX ON users (username);

CREATE TABLE tournaments (
       id             serial,
       start_date     timestamp,
       end_date       timestamp,
       name           text,
       registration_open boolean,
       PRIMARY KEY(id)
);

CREATE INDEX ON tournaments (end_date);
CREATE INDEX ON tournaments (start_date);

CREATE TABLE events (
       id             serial,
       tournament_id  integer REFERENCES tournaments(id),
       name           text,
       mode           text,
       team_mode      boolean,
       PRIMARY KEY(id)
);

CREATE INDEX ON events (tournament_id);

CREATE TABLE registrations (
       id             serial,
       event_id       integer REFERENCES events(id),
       user_id        integer REFERENCES users(id),
       team           text,
       date           timestamp,
       PRIMARY KEY(id),
       UNIQUE(user_id, event_id)
);

CREATE INDEX ON registrations (event_id);
CREATE INDEX ON registrations (user_id);
