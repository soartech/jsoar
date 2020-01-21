# semantic_memory.cpp:statement_container::statement_container
# These are all the add_structure statements for initializing the smem 
# database. All statements *must* be on a single line. Lines starting with #
# are comments. If a line starts with [XXX], then that line is only executed
# if the db driver (JDBC class name) is XXX.

## semantic_memory.cpp:statement_container::create_tables()

CREATE TABLE IF NOT EXISTS versions (system TEXT PRIMARY KEY,version_number TEXT)
CREATE TABLE IF NOT EXISTS @PREFIX@persistent_variables (variable_id INTEGER PRIMARY KEY,variable_value INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@symbols_type (s_id INTEGER PRIMARY KEY, symbol_type INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@symbols_integer (s_id INTEGER PRIMARY KEY, symbol_value INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@symbols_float (s_id INTEGER PRIMARY KEY, symbol_value REAL)
CREATE TABLE IF NOT EXISTS @PREFIX@symbols_string (s_id INTEGER PRIMARY KEY, symbol_value TEXT)
CREATE TABLE IF NOT EXISTS @PREFIX@lti (lti_id INTEGER PRIMARY KEY, soar_letter INTEGER, soar_number INTEGER, total_augmentations INTEGER, activation_value REAL, activations_total INTEGER, activations_last INTEGER, activations_first INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@activation_history (lti_id INTEGER PRIMARY KEY, t1 INTEGER, t2 INTEGER, t3 INTEGER, t4 INTEGER, t5 INTEGER, t6 INTEGER, t7 INTEGER, t8 INTEGER, t9 INTEGER, t10 INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@augmentations (lti_id INTEGER, attribute_s_id INTEGER, value_constant_s_id INTEGER, value_lti_id INTEGER, activation_value REAL)
CREATE TABLE IF NOT EXISTS @PREFIX@attribute_frequency (attribute_s_id INTEGER PRIMARY KEY, edge_frequency INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@wmes_constant_frequency (attribute_s_id INTEGER, value_constant_s_id INTEGER, edge_frequency INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@wmes_lti_frequency (attribute_s_id INTEGER, value_lti_id INTEGER, edge_frequency INTEGER)

# adding an ascii table just to make lti queries easier when inspecting database
CREATE TABLE IF NOT EXISTS @PREFIX@ascii (ascii_num INTEGER PRIMARY KEY, ascii_chr TEXT)

INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (65,'A')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (66,'B')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (67,'C')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (68,'D')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (69,'E')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (70,'F')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (71,'G')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (72,'H')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (73,'I')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (74,'J')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (75,'K')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (76,'L')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (77,'M')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (78,'N')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (79,'O')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (80,'P')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (81,'Q')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (82,'R')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (83,'S')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (84,'T')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (85,'U')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (86,'V')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (87,'W')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (88,'X')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (89,'Y')
INSERT OR IGNORE INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (90,'Z')

## semantic_memory.cpp:statement_container::create_indices()

CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@symbols_int_const ON @PREFIX@symbols_integer (symbol_value)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@symbols_float_const ON @PREFIX@symbols_float (symbol_value)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@symbols_str_const ON @PREFIX@symbols_string (symbol_value)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@lti_letter_num ON @PREFIX@lti (soar_letter, soar_number)
CREATE INDEX IF NOT EXISTS @PREFIX@lti_t ON @PREFIX@lti (activations_last)
CREATE INDEX IF NOT EXISTS @PREFIX@augmentations_parent_attr_val_lti ON @PREFIX@augmentations (lti_id, attribute_s_id, value_constant_s_id, value_lti_id)
CREATE INDEX IF NOT EXISTS @PREFIX@augmentations_attr_val_lti_cycle ON @PREFIX@augmentations (attribute_s_id, value_constant_s_id, value_lti_id, activation_value)
CREATE INDEX IF NOT EXISTS @PREFIX@augmentations_attr_cycle ON @PREFIX@augmentations (attribute_s_id, activation_value)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@wmes_constant_frequency_attr_val ON @PREFIX@wmes_constant_frequency (attribute_s_id, value_constant_s_id)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@ct_lti_attr_val ON @PREFIX@wmes_lti_frequency (attribute_s_id, value_lti_id)
