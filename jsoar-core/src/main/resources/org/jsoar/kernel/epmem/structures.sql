# episodic_memory.cpp:epem_statement_container::epmem_statement_container
# These are all the add_structure statements for initializing the epmem 
# database. All statements *must* be on a single line. Lines starting with #
# are comments. If a line starts with [XXX], then that line is only executed
# if the db driver (JDBC class name) is XXX.

# @PREFIX@epmem_common_statement_container
# create_graph_tables
CREATE TABLE IF NOT EXISTS @PREFIX@versions (system TEXT PRIMARY KEY,version_number TEXT)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_persistent_variables (variable_id INTEGER PRIMARY KEY,variable_value NONE)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_rit_left_nodes (rit_min INTEGER, rit_max INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_rit_right_nodes (rit_id INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_symbols_type (s_id INTEGER PRIMARY KEY, symbol_type INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_symbols_integer (s_id INTEGER PRIMARY KEY, symbol_value INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_symbols_float (s_id INTEGER PRIMARY KEY, symbol_value REAL)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_symbols_string (s_id INTEGER PRIMARY KEY, symbol_value TEXT)

# create_graph_indices
CREATE UNIQUE INDEX IF NOT EXISTS symbols_int_const ON @PREFIX@epmem_symbols_integer (symbol_value)
CREATE UNIQUE INDEX IF NOT EXISTS symbols_float_const ON @PREFIX@epmem_symbols_float (symbol_value)
CREATE UNIQUE INDEX IF NOT EXISTS symbols_str_const ON @PREFIX@epmem_symbols_string (symbol_value)

############################################################################
# @PREFIX@epmem_graph_statement_container

# create_graph_tables

CREATE TABLE IF NOT EXISTS @PREFIX@epmem_nodes (n_id INTEGER PRIMARY KEY)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_episodes (episode_id INTEGER PRIMARY KEY)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_wmes_constant_now (wc_id INTEGER,start_episode_id INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_wmes_identifier_now (wi_id INTEGER,start_episode_id INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_wmes_constant_point (wc_id INTEGER,episode_id INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_wmes_identifier_point (wi_id INTEGER,episode_id INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_wmes_constant_range (rit_id INTEGER,start_episode_id INTEGER,end_episode_id INTEGER,wc_id INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_wmes_identifier_range (rit_id INTEGER,start_episode_id INTEGER,end_episode_id INTEGER,wi_id INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_wmes_constant (wc_id INTEGER PRIMARY KEY AUTOINCREMENT,parent_n_id INTEGER,attribute_s_id INTEGER, value_s_id INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_wmes_identifier (wi_id INTEGER PRIMARY KEY AUTOINCREMENT,parent_n_id INTEGER,attribute_s_id INTEGER,child_n_id INTEGER, last_episode_id INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_lti (n_id INTEGER PRIMARY KEY, soar_letter INTEGER, soar_number INTEGER, promotion_episode_id INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@epmem_ascii (ascii_num INTEGER PRIMARY KEY, ascii_chr TEXT)

# create_graph_indices

CREATE INDEX IF NOT EXISTS epmem_wmes_constant_now_start ON @PREFIX@epmem_wmes_constant_now (start_episode_id)
CREATE UNIQUE INDEX IF NOT EXISTS epmem_wmes_constant_now_id_start ON @PREFIX@epmem_wmes_constant_now (wc_id,start_episode_id DESC)
CREATE INDEX IF NOT EXISTS epmem_wmes_identifier_now_start ON @PREFIX@epmem_wmes_identifier_now (start_episode_id)
CREATE UNIQUE INDEX IF NOT EXISTS epmem_wmes_identifier_now_id_start ON @PREFIX@epmem_wmes_identifier_now (wi_id,start_episode_id DESC)

CREATE UNIQUE INDEX IF NOT EXISTS epmem_wmes_constant_point_id_start ON @PREFIX@epmem_wmes_constant_point (wc_id,episode_id DESC)
CREATE INDEX IF NOT EXISTS epmem_wmes_constant_point_start ON @PREFIX@epmem_wmes_constant_point (episode_id)

CREATE UNIQUE INDEX IF NOT EXISTS epmem_wmes_identifier_point_id_start ON @PREFIX@epmem_wmes_identifier_point (wi_id,episode_id DESC)
CREATE INDEX IF NOT EXISTS epmem_wmes_identifier_point_start ON @PREFIX@epmem_wmes_identifier_point (episode_id)

CREATE INDEX IF NOT EXISTS epmem_wmes_constant_range_lower ON @PREFIX@epmem_wmes_constant_range (rit_id,start_episode_id)
CREATE INDEX IF NOT EXISTS epmem_wmes_constant_range_upper ON @PREFIX@epmem_wmes_constant_range (rit_id,end_episode_id)
CREATE UNIQUE INDEX IF NOT EXISTS epmem_wmes_constant_range_id_start ON @PREFIX@epmem_wmes_constant_range (wc_id,start_episode_id DESC)
CREATE UNIQUE INDEX IF NOT EXISTS epmem_wmes_constant_range_id_end_start ON @PREFIX@epmem_wmes_constant_range (wc_id,end_episode_id DESC,start_episode_id)

CREATE INDEX IF NOT EXISTS epmem_wmes_identifier_range_lower ON @PREFIX@epmem_wmes_identifier_range (rit_id,start_episode_id)
CREATE INDEX IF NOT EXISTS epmem_wmes_identifier_range_upper ON @PREFIX@epmem_wmes_identifier_range (rit_id,end_episode_id)
CREATE UNIQUE INDEX IF NOT EXISTS epmem_wmes_identifier_range_id_start ON @PREFIX@epmem_wmes_identifier_range (wi_id,start_episode_id DESC)
CREATE UNIQUE INDEX IF NOT EXISTS epmem_wmes_identifier_range_id_end_start ON @PREFIX@epmem_wmes_identifier_range (wi_id,end_episode_id DESC,start_episode_id)

CREATE UNIQUE INDEX IF NOT EXISTS epmem_wmes_constant_parent_attribute_value ON @PREFIX@epmem_wmes_constant (parent_n_id,attribute_s_id,value_s_id)

CREATE INDEX IF NOT EXISTS epmem_wmes_identifier_parent_attribute_last_id ON @PREFIX@epmem_wmes_identifier (parent_n_id,attribute_s_id,last_episode_id)
CREATE UNIQUE INDEX IF NOT EXISTS epmem_wmes_identifier_parent_attribute_child ON @PREFIX@epmem_wmes_identifier (parent_n_id,attribute_s_id,child_n_id)

CREATE UNIQUE INDEX IF NOT EXISTS epmem_lti_letter_num ON @PREFIX@epmem_lti (soar_letter,soar_number)

# adding an ascii table just to make lti queries easier when inspecting database
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (65,'A')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (66,'B')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (67,'C')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (68,'D')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (69,'E')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (70,'F')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (71,'G')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (72,'H')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (73,'I')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (74,'J')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (75,'K')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (76,'L')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (77,'M')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (78,'N')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (79,'O')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (80,'P')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (81,'Q')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (82,'R')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (83,'S')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (84,'T')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (85,'U')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (86,'V')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (87,'W')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (88,'X')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (89,'Y')
INSERT OR IGNORE INTO @PREFIX@epmem_ascii (ascii_num, ascii_chr) VALUES (90,'Z')

# workaround for tree: type 1 = IDENTIFIER_SYMBOL_TYPE 
INSERT OR IGNORE INTO @PREFIX@epmem_nodes (n_id) VALUES (0)

# Root node of tree
# Note:  I dont think root node string is ever actually looked up.  Set to root instead of
#        previous NULL for compatibility with other db systems.
INSERT OR IGNORE INTO @PREFIX@epmem_symbols_type (s_id,symbol_type) VALUES (0,2)
INSERT OR IGNORE INTO @PREFIX@epmem_symbols_string (s_id,symbol_value) VALUES (0,'root')

# Acceptable preference wmes: id 1 = "operator+"
INSERT OR IGNORE INTO @PREFIX@epmem_symbols_type (s_id,symbol_type) VALUES (1,2)
INSERT OR IGNORE INTO @PREFIX@epmem_symbols_string (s_id,symbol_value) VALUES (1,'operator*')

# Finally, create the "signature" table that we use to decide whether
# the db structure is already initialized
CREATE TABLE @PREFIX@epmem_signature (uid INTEGER)
