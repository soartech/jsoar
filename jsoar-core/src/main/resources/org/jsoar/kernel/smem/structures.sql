# semantic_memory.cpp:smem_statement_container::smem_statement_container
# These are all the add_structure statements for initializing the smem 
# database. All statements *must* be on a single line. Lines starting with #
# are comments. If a line starts with [XXX], then that line is only executed
# if the db driver (JDBC class name) is XXX.

# TODO support other types for values.
CREATE TABLE @PREFIX@vars (id INTEGER PRIMARY KEY,value INTEGER)

# SQLite
[org.sqlite.JDBC] CREATE TABLE @PREFIX@symbols_type (id INTEGER PRIMARY KEY, sym_type INTEGER)
# MySQL
[com.mysql.jdbc.Driver] CREATE TABLE @PREFIX@symbols_type (id INTEGER PRIMARY KEY AUTO_INCREMENT, sym_type INTEGER)
# PostgreSQL
[org.postgresql.Driver] CREATE TABLE @PREFIX@symbols_type (id SERIAL, sym_type INTEGER, PRIMARY KEY (id))

CREATE TABLE @PREFIX@symbols_int (id INTEGER PRIMARY KEY, sym_const INTEGER)
CREATE UNIQUE INDEX @PREFIX@symbols_int_const ON @PREFIX@symbols_int (sym_const)

CREATE TABLE @PREFIX@symbols_float (id INTEGER PRIMARY KEY, sym_const REAL)
CREATE UNIQUE INDEX @PREFIX@symbols_float_const ON @PREFIX@symbols_float (sym_const)

# SQLite
[org.sqlite.JDBC] CREATE TABLE @PREFIX@symbols_str (id INTEGER PRIMARY KEY, sym_const TEXT)
# MySQL
[com.mysql.jdbc.Driver] CREATE TABLE @PREFIX@symbols_str (id INTEGER PRIMARY KEY, sym_const VARCHAR(255))
# PostgreSQL
[org.postgresql.Driver] CREATE TABLE @PREFIX@symbols_str (id INTEGER PRIMARY KEY, sym_const TEXT)

CREATE UNIQUE INDEX @PREFIX@symbols_str_const ON @PREFIX@symbols_str (sym_const)

# SQLite
[org.sqlite.JDBC] CREATE TABLE @PREFIX@lti (id INTEGER PRIMARY KEY, letter INTEGER, num INTEGER, child_ct INTEGER, act_cycle INTEGER)
# MySQL
[com.mysql.jdbc.Driver] CREATE TABLE @PREFIX@lti (id INTEGER PRIMARY KEY AUTO_INCREMENT, letter INTEGER, num INTEGER, child_ct INTEGER, act_cycle INTEGER)
# PostgreSQL
[org.postgresql.Driver] CREATE TABLE @PREFIX@lti (id SERIAL, letter INTEGER, num INTEGER, child_ct INTEGER, act_cycle INTEGER, PRIMARY KEY (id))

CREATE UNIQUE INDEX @PREFIX@lti_letter_num ON @PREFIX@lti (letter, num)

CREATE TABLE @PREFIX@web (parent_id INTEGER, attr INTEGER, val_const INTEGER, val_lti INTEGER, act_cycle INTEGER)
CREATE INDEX @PREFIX@web_parent_attr_val_lti ON @PREFIX@web (parent_id, attr, val_const, val_lti)
CREATE INDEX @PREFIX@web_attr_val_lti_cycle ON @PREFIX@web (attr, val_const, val_lti, act_cycle)
CREATE INDEX @PREFIX@web_attr_cycle ON @PREFIX@web (attr, act_cycle)

CREATE TABLE @PREFIX@ct_attr (attr INTEGER PRIMARY KEY, ct INTEGER)

CREATE TABLE @PREFIX@ct_const (attr INTEGER, val_const INTEGER, ct INTEGER)
CREATE UNIQUE INDEX @PREFIX@ct_const_attr_val ON @PREFIX@ct_const (attr, val_const)

CREATE TABLE @PREFIX@ct_lti (attr INTEGER, val_lti INTEGER, ct INTEGER)
CREATE UNIQUE INDEX @PREFIX@ct_lti_attr_val ON @PREFIX@ct_lti (attr, val_lti) 

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

# Finally, create the "signature" table that we use to decide whether
# the db structure is already initialized
CREATE TABLE @PREFIX@signature (uid INTEGER)
