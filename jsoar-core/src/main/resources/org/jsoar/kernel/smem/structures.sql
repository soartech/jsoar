# semantic_memory.cpp:smem_statement_container::smem_statement_container
# These are all the add_structure statements for initializing the smem 
# database. All statements *must* be on a single line. Lines starting with #
# are comments. If a line starts with [XXX], then that line is only executed
# if the db driver (JDBC class name) is XXX.

## semantic_memory.cpp:smem_statement_container::create_tables()

CREATE TABLE IF NOT EXISTS @PREFIX@versions (system TEXT PRIMARY KEY,version_number TEXT)
CREATE TABLE @PREFIX@smem_persistent_variables (variable_id INTEGER PRIMARY KEY,variable_value INTEGER)
CREATE TABLE @PREFIX@smem_symbols_type (s_id INTEGER PRIMARY KEY, symbol_type INTEGER)
CREATE TABLE @PREFIX@smem_symbols_integer (s_id INTEGER PRIMARY KEY, symbol_value INTEGER)
CREATE TABLE @PREFIX@smem_symbols_float (s_id INTEGER PRIMARY KEY, symbol_value REAL)
CREATE TABLE @PREFIX@smem_symbols_string (s_id INTEGER PRIMARY KEY, symbol_value TEXT)
CREATE TABLE @PREFIX@smem_lti (lti_id INTEGER PRIMARY KEY, soar_letter INTEGER, soar_number INTEGER, total_augmentations INTEGER, activation_value REAL, activations_total INTEGER, activations_last INTEGER, activations_first INTEGER)
CREATE TABLE @PREFIX@smem_activation_history (lti_id INTEGER PRIMARY KEY, t1 INTEGER, t2 INTEGER, t3 INTEGER, t4 INTEGER, t5 INTEGER, t6 INTEGER, t7 INTEGER, t8 INTEGER, t9 INTEGER, t10 INTEGER)
CREATE TABLE @PREFIX@smem_augmentations (lti_id INTEGER, attribute_s_id INTEGER, value_constant_s_id INTEGER, value_lti_id INTEGER, activation_value REAL)
CREATE TABLE @PREFIX@smem_attribute_frequency (attribute_s_id INTEGER PRIMARY KEY, edge_frequency INTEGER)
CREATE TABLE @PREFIX@smem_wmes_constant_frequency (attribute_s_id INTEGER, value_constant_s_id INTEGER, edge_frequency INTEGER)
CREATE TABLE @PREFIX@smem_wmes_lti_frequency (attribute_s_id INTEGER, value_lti_id INTEGER, edge_frequency INTEGER)

# adding an ascii table just to make lti queries easier when inspecting database
CREATE TABLE @PREFIX@ascii (ascii_num INTEGER PRIMARY KEY, ascii_chr TEXT)
DELETE FROM @PREFIX@ascii

INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (65,'A')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (66,'B')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (67,'C')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (68,'D')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (69,'E')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (70,'F')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (71,'G')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (72,'H')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (73,'I')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (74,'J')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (75,'K')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (76,'L')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (77,'M')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (78,'N')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (79,'O')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (80,'P')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (81,'Q')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (82,'R')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (83,'S')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (84,'T')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (85,'U')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (86,'V')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (87,'W')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (88,'X')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (89,'Y')
INSERT INTO @PREFIX@ascii (ascii_num, ascii_chr) VALUES (90,'Z')

## semantic_memory.cpp:smem_statement_container::create_indices()

CREATE UNIQUE INDEX @PREFIX@smem_symbols_int_const ON @PREFIX@smem_symbols_integer (symbol_value)
CREATE UNIQUE INDEX @PREFIX@smem_symbols_float_const ON @PREFIX@smem_symbols_float (symbol_value)
CREATE UNIQUE INDEX @PREFIX@smem_symbols_str_const ON @PREFIX@smem_symbols_string (symbol_value)
CREATE UNIQUE INDEX @PREFIX@smem_lti_letter_num ON @PREFIX@smem_lti (soar_letter, soar_number)
CREATE INDEX @PREFIX@smem_lti_t ON @PREFIX@smem_lti (activations_last)
CREATE INDEX @PREFIX@smem_augmentations_parent_attr_val_lti ON @PREFIX@smem_augmentations (lti_id, attribute_s_id, value_constant_s_id, value_lti_id)
CREATE INDEX @PREFIX@smem_augmentations_attr_val_lti_cycle ON @PREFIX@smem_augmentations (attribute_s_id, value_constant_s_id, value_lti_id, activation_value)
CREATE INDEX @PREFIX@smem_augmentations_attr_cycle ON @PREFIX@smem_augmentations (attribute_s_id, activation_value)
CREATE UNIQUE INDEX @PREFIX@smem_wmes_constant_frequency_attr_val ON @PREFIX@smem_wmes_constant_frequency (attribute_s_id, value_constant_s_id)
CREATE UNIQUE INDEX @PREFIX@smem_ct_lti_attr_val ON @PREFIX@smem_wmes_lti_frequency (attribute_s_id, value_lti_id)
