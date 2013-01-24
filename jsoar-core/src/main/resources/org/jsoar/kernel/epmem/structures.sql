# episodic_memory.cpp:epem_statement_container::epmem_statement_container
# These are all the add_structure statements for initializing the epmem 
# database. All statements *must* be on a single line. Lines starting with #
# are comments. If a line starts with [XXX], then that line is only executed
# if the db driver (JDBC class name) is XXX.

# epmem_common_statement_container
CREATE TABLE IF NOT EXISTS @PREFIX@vars (id INTEGER PRIMARY KEY,value NONE)
CREATE TABLE IF NOT EXISTS @PREFIX@rit_left_nodes (min INTEGER, max INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@rit_right_nodes (node INTEGER)
CREATE TABLE IF NOT EXISTS @PREFIX@temporal_symbol_hash (id INTEGER PRIMARY KEY, sym_const NONE, sym_type INTEGER)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@temporal_symbol_hash_const_type ON @PREFIX@temporal_symbol_hash (sym_type,sym_const)
  
# workaround for tree: type 1 = IDENTIFIER_SYMBOL_TYPE 
INSERT OR IGNORE INTO @PREFIX@temporal_symbol_hash (id,sym_const,sym_type) VALUES (0,NULL,1)

# workaround for acceptable preference wmes: id 1 = "operator+"
INSERT OR IGNORE INTO @PREFIX@temporal_symbol_hash (id,sym_const,sym_type) VALUES (1,'operator*',2)


############################################################################
# epmem_graph_statement_container

CREATE TABLE IF NOT EXISTS @PREFIX@times (id INTEGER PRIMARY KEY)

CREATE TABLE IF NOT EXISTS @PREFIX@node_now (id INTEGER,start INTEGER)
CREATE INDEX IF NOT EXISTS @PREFIX@node_now_start ON @PREFIX@node_now (start)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@node_now_id_start ON @PREFIX@node_now (id,start DESC)

CREATE TABLE IF NOT EXISTS @PREFIX@edge_now (id INTEGER,start INTEGER)
CREATE INDEX IF NOT EXISTS @PREFIX@edge_now_start ON @PREFIX@edge_now (start)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@edge_now_id_start ON @PREFIX@edge_now (id,start DESC)

CREATE TABLE IF NOT EXISTS @PREFIX@node_point (id INTEGER,start INTEGER)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@node_point_id_start ON @PREFIX@node_point (id,start DESC)
CREATE INDEX IF NOT EXISTS @PREFIX@node_point_start ON @PREFIX@node_point (start)

CREATE TABLE IF NOT EXISTS @PREFIX@edge_point (id INTEGER,start INTEGER)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@edge_point_id_start ON @PREFIX@edge_point (id,start DESC)
CREATE INDEX IF NOT EXISTS @PREFIX@edge_point_start ON @PREFIX@edge_point (start)

CREATE TABLE IF NOT EXISTS @PREFIX@node_range (rit_node INTEGER,start INTEGER,end INTEGER,id INTEGER)
CREATE INDEX IF NOT EXISTS @PREFIX@node_range_lower ON @PREFIX@node_range (rit_node,start)
CREATE INDEX IF NOT EXISTS @PREFIX@node_range_upper ON @PREFIX@node_range (rit_node,end)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@node_range_id_start ON @PREFIX@node_range (id,start DESC)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@node_range_id_end ON @PREFIX@node_range (id,end DESC)

CREATE TABLE IF NOT EXISTS @PREFIX@edge_range (rit_node INTEGER,start INTEGER,end INTEGER,id INTEGER)
CREATE INDEX IF NOT EXISTS @PREFIX@edge_range_lower ON @PREFIX@edge_range (rit_node,start)
CREATE INDEX IF NOT EXISTS @PREFIX@edge_range_upper ON @PREFIX@edge_range (rit_node,end)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@edge_range_id_start ON @PREFIX@edge_range (id,start DESC)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@edge_range_id_end ON @PREFIX@edge_range (id,end DESC)

CREATE TABLE IF NOT EXISTS @PREFIX@node_unique (child_id INTEGER PRIMARY KEY AUTOINCREMENT,parent_id INTEGER,attrib INTEGER, value INTEGER)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@node_unique_parent_attrib_value ON @PREFIX@node_unique (parent_id,attrib,value)

CREATE TABLE IF NOT EXISTS @PREFIX@edge_unique (parent_id INTEGER PRIMARY KEY AUTOINCREMENT,q0 INTEGER,w INTEGER,q1 INTEGER,last INTEGER)
CREATE INDEX IF NOT EXISTS @PREFIX@edge_unique_q0_w_q1 ON @PREFIX@edge_unique (q0,w,q1)

CREATE TABLE IF NOT EXISTS @PREFIX@lti (parent_id INTEGER PRIMARY KEY, letter INTEGER, num INTEGER, time_id INTEGER)
CREATE UNIQUE INDEX IF NOT EXISTS @PREFIX@lti_letter_num ON @PREFIX@lti (letter,num)

# adding an ascii table just to make lti queries easier when inspecting database
CREATE TABLE IF NOT EXISTS @PREFIX@ascii (ascii_num INTEGER PRIMARY KEY, ascii_chr TEXT)
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


# Finally, create the "signature" table that we use to decide whether
# the db structure is already initialized
CREATE TABLE @PREFIX@signature (uid INTEGER)
