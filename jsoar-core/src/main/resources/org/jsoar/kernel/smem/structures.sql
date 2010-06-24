# semantic_memory.cpp:smem_statement_container::smem_statement_container
# These are all the add_structure statements for initializing the smem 
# database
CREATE TABLE IF NOT EXISTS vars (id INTEGER PRIMARY KEY,value NONE)
CREATE TABLE IF NOT EXISTS temporal_symbol_hash (id INTEGER PRIMARY KEY, sym_const NONE, sym_type INTEGER)
CREATE UNIQUE INDEX IF NOT EXISTS temporal_symbol_hash_const_type ON temporal_symbol_hash (sym_type,sym_const)

CREATE TABLE IF NOT EXISTS lti (id INTEGER PRIMARY KEY, letter INTEGER, num INTEGER, child_ct INTEGER, act_cycle INTEGER)
CREATE UNIQUE INDEX IF NOT EXISTS lti_letter_num ON lti (letter, num)

CREATE TABLE IF NOT EXISTS web (parent_id INTEGER, attr INTEGER, val_const INTEGER, val_lti INTEGER, act_cycle INTEGER)
CREATE INDEX IF NOT EXISTS web_parent_attr_val_lti ON web (parent_id, attr, val_const, val_lti)
CREATE INDEX IF NOT EXISTS web_attr_val_lti_cycle ON web (attr, val_const, val_lti, act_cycle)
CREATE INDEX IF NOT EXISTS web_attr_cycle ON web (attr, act_cycle)

CREATE TABLE IF NOT EXISTS ct_attr (attr INTEGER PRIMARY KEY, ct INTEGER)

CREATE TABLE IF NOT EXISTS ct_const (attr INTEGER, val_const INTEGER, ct INTEGER)
CREATE UNIQUE INDEX IF NOT EXISTS ct_const_attr_val ON ct_const (attr, val_const)

CREATE TABLE IF NOT EXISTS ct_lti (attr INTEGER, val_lti INTEGER, ct INTEGER)
CREATE UNIQUE INDEX IF NOT EXISTS ct_lti_attr_val ON ct_lti (attr, val_lti) 

# adding an ascii table just to make lti queries easier when inspecting database
CREATE TABLE IF NOT EXISTS ascii (ascii_num INTEGER PRIMARY KEY, ascii_chr TEXT)
DELETE FROM ascii

INSERT INTO ascii (ascii_num, ascii_chr) VALUES (65,'A')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (66,'B')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (67,'C')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (68,'D')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (69,'E')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (70,'F')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (71,'G')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (72,'H')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (73,'I')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (74,'J')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (75,'K')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (76,'L')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (77,'M')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (78,'N')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (79,'O')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (80,'P')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (81,'Q')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (82,'R')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (83,'S')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (84,'T')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (85,'U')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (86,'V')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (87,'W')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (88,'X')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (89,'Y')
INSERT INTO ascii (ascii_num, ascii_chr) VALUES (90,'Z')

