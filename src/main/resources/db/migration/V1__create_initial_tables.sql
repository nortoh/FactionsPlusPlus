CREATE TABLE default_flags (
    name CHAR(255),
    description TEXT,
    type TINYINT,
    expected_data_type VARCHAR(255),
    default_value VARCHAR(255),
    PRIMARY KEY (name)
);

CREATE TABLE factions (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    prefix VARCHAR(255),
    bonus_power DOUBLE,
    should_autoclaim BOOLEAN NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY f_unique_name (name)
);

CREATE TABLE worlds (
    id BINARY(16) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE faction_bases (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    faction_id BINARY(16) NOT NULL,
    world_id BINARY(16) NOT NULL,
    x_position DOUBLE NOT NULL,
    y_position DOUBLE NOT NULL,
    z_position DOUBLE NOT NULL,
    allow_all_members BOOLEAN NOT NULL DEFAULT 0,
    allow_allies BOOLEAN NOT NULL DEFAULT 0,
    is_faction_default BOOLEAN NOT NULL DEFAULT 0,
    PRIMARY KEY(id),
    UNIQUE KEY fb_unique_location (world_id, x_position, y_position, z_position),
    UNIQUE KEY fb_unique_name (name, faction_id),
    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE,
    FOREIGN KEY(world_id) REFERENCES worlds(id) ON DELETE CASCADE
);

CREATE TABLE faction_flags (
    faction_id BINARY(16) NOT NULL,
    flag_name CHAR(255) NOT NULL,
    `value` VARCHAR(255),
    PRIMARY KEY(faction_id, flag_name),
    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE,
    FOREIGN KEY(flag_name) REFERENCES default_flags(name) ON DELETE CASCADE
);

CREATE TABLE world_flags (
    world_id BINARY(16) NOT NULL,
    flag_name CHAR(255) NOT NULL,
    `value` VARCHAR(255),
    PRIMARY KEY(world_id, flag_name),
    FOREIGN KEY(world_id) REFERENCES worlds(id) ON DELETE CASCADE,
    FOREIGN KEY(flag_name) REFERENCES default_flags(name) ON DELETE CASCADE
);

CREATE TABLE players (
    id BINARY(16) NOT NULL,
    power DOUBLE NOT NULL DEFAULT 0,
    is_admin_bypassing BOOLEAN NOT NULL DEFAULT 0,
    login_count INTEGER NOT NULL DEFAULT 1,
    last_logout DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    offline_power_lost DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE faction_members (
    faction_id BINARY(16) NOT NULL,
    player_id BINARY(16) NOT NULL,
    role INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY(faction_id, player_id),
    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE
);

CREATE TABLE claimed_chunks (
    faction_id BINARY(16) NOT NULL,
    world_id BINARY(16) NOT NULL,
    x_position INTEGER NOT NULL,
    z_position INTEGER NOT NULL,
    PRIMARY KEY(world_id, x_position, z_position),
    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE,
    FOREIGN KEY(world_id) REFERENCES worlds(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS faction_wars (
    id BINARY(16) NOT NULL,
    attacker_id BINARY(16) NOT NULL,
    defender_id BINARY(16) NOT NULL,
    reason VARCHAR(1024),
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    PRIMARY KEY(id),
    FOREIGN KEY(attacker_id) REFERENCES factions(id) ON DELETE CASCADE,
    FOREIGN KEY(defender_id) REFERENCES factions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS locked_blocks (
    id BINARY(16) NOT NULL,
    world_id BINARY(16) NOT NULL,
    faction_id BINARY(16) NOT NULL,
    x_position INTEGER NOT NULL,
    y_position INTEGER NOT NULL,
    z_position INTEGER NOT NULL,
    player_id BINARY(16) NOT NULL,
    allow_allies BOOLEAN NOT NULL DEFAULT 0,
    allow_faction_members BOOLEAN NOT NULL DEFAULT 0,
    PRIMARY KEY(id),
    UNIQUE KEY UNIQUE_POSITION (world_id, x_position, y_position, z_position),
    FOREIGN KEY(world_id) REFERENCES worlds(id) ON DELETE CASCADE,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE,
    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS locked_block_access_list (
    locked_block_id BINARY(16) NOT NULL,
    player_id BINARY(16) NOT NULL,
    PRIMARY KEY (locked_block_id, player_id),
    FOREIGN KEY(locked_block_id) REFERENCES locked_blocks(id) ON DELETE CASCADE,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS faction_gates (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    faction_id BINARY(16) NOT NULL,
    material CHAR(255) NOT NULL,
    world_id BINARY(16) NOT NULL,
    position_one_location JSON,
    position_two_location JSON,
    trigger_location JSON,
    is_vertical BOOLEAN NOT NULL DEFAULT 0,
    is_open BOOLEAN NOT NULL DEFAULT 0,
    PRIMARY KEY(id),
    FOREIGN KEY(world_id) REFERENCES worlds(id) ON DELETE CASCADE,
    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE
);

CREATE TABLE faction_invites (
    player_id BINARY(16) NOT NULL,
    faction_id BINARY(16) NOT NULL,
    invited_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(player_id, faction_id),
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE,
    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE
);

CREATE TABLE faction_relations (
    source_faction BINARY(16) NOT NULL,
    target_faction BINARY(16) NOT NULL,
    type TINYINT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(source_faction, target_faction),
    FOREIGN KEY(source_faction) REFERENCES factions(id) ON DELETE CASCADE,
    FOREIGN KEY(target_faction) REFERENCES factions(id) ON DELETE CASCADE
);

CREATE TABLE faction_laws (
    id BINARY(16) NOT NULL,
    faction_id BINARY(16) NOT NULL,
    text TEXT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE
);