# SQLITE Performance Setting. Extracted from semantic_memory.cpp:1952:smem_init_db

# synchronous - don't wait for writes to complete (can corrupt the db in case unexpected crash during transaction)
PRAGMA synchronous = OFF
    
# journal_mode - no atomic transactions (can result in database corruption if crash during transaction)
PRAGMA journal_mode = OFF
                    
# locking_mode - no one else can view the database after our first write
PRAGMA locking_mode = EXCLUSIVE
