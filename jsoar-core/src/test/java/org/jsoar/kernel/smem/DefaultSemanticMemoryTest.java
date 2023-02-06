/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2010
 */
package org.jsoar.kernel.smem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.PrintWriter;
import java.sql.Connection;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.parser.original.Lexeme;
import org.jsoar.kernel.parser.original.LexemeType;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.AdaptableContainer;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.properties.PropertyManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultSemanticMemoryTest
{
    private AdaptableContainer context;
    private Connection conn;
    private DefaultSemanticMemory smem;
    
    @BeforeEach
    public void setUp() throws Exception
    {
        context = AdaptableContainer.from(new SymbolFactoryImpl(), new PropertyManager(), new Agent());
        conn = JdbcTools.connect("org.sqlite.JDBC", "jdbc:sqlite::memory:");
        final SemanticMemoryDatabase db = new SemanticMemoryDatabase("org.sqlite.JDBC", conn);
        db.structure();
        db.prepare();
        smem = new DefaultSemanticMemory(context, db);
        smem.initialize();
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        conn.close();
    }

    @Test
    public void testCanAddALongTermIdentifier() throws Exception
    {
        final long lti = smem.smem_lti_add_id('Z', 99);
        assertEquals(1, lti);
    }
    
    @Test
    public void testCanRetrieveALongTermIdentifier() throws Exception
    {
        final long expected = smem.smem_lti_add_id('Z', 99);
        assertEquals(expected, smem.smem_lti_get_id('Z', 99));
        
        final long expected2 = smem.smem_lti_add_id('S', 2);
        assertEquals(expected2, smem.smem_lti_get_id('S', 2));
        assertFalse(expected == expected2);
    }

    @Test
    public void testCanResetIdCountersInSymbolFactory() throws Exception
    {
        long number = 1; 
        for(char letter = 'A'; letter <= 'Z'; letter++)
        {
            smem.smem_lti_add_id(letter, number++);
        }
        
        final SymbolFactoryImpl syms = Adaptables.adapt(context, SymbolFactoryImpl.class);
        smem.smem_reset_id_counters();
        
        long expected_number = 1;
        for(char letter = 'A'; letter <= 'Z'; letter++)
        {
            assertEquals(expected_number + 1, syms.getIdNumber(letter));
            expected_number++;
        }
    }
    
    @Test
    public void testCanInitializeTheDatabase() throws Exception
    {
        final DefaultSemanticMemory smem = new DefaultSemanticMemory(context);
        smem.initialize();
        assertNull(smem.getDatabase());
        smem.smem_attach();
        assertNotNull(smem.getDatabase());
        assertFalse(smem.getDatabase().getConnection().isClosed());
    }
    
    @Test
    public void testCanParseAnLtiNameForIdentifierLexeme()
    {
        final Lexeme lexeme = new Lexeme();
        lexeme.type = LexemeType.IDENTIFIER;
        lexeme.string = "Z99";
        lexeme.id_letter = 'Z';
        lexeme.id_number = 99;
        
        final ParsedLtiName parsed = DefaultSemanticMemory.smem_parse_lti_name(lexeme);
        assertNotNull(parsed);
        assertEquals("Z99", parsed.value);
        assertEquals('Z', parsed.id_letter);
        assertEquals(99, parsed.id_number);
    }
    
    @Test
    public void testCanParseAnLtiNameForNonIdentifierLexeme()
    {
        final Lexeme lexeme = new Lexeme();
        lexeme.type = LexemeType.VARIABLE;
        lexeme.string = "<yumyum>";
        
        final ParsedLtiName parsed = DefaultSemanticMemory.smem_parse_lti_name(lexeme);
        assertNotNull(parsed);
        assertEquals("<yumyum>", parsed.value);
        assertEquals('Y', parsed.id_letter);
        assertEquals(0, parsed.id_number);
    }
    
    @Test
    public void testCanParseAStringConstant()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final Lexeme lexeme = new Lexeme();
        lexeme.type = LexemeType.SYM_CONSTANT;
        lexeme.string = "yumyum";
        
        final SymbolImpl result = DefaultSemanticMemory.smem_parse_constant_attr(syms, lexeme);
        assertNotNull(result);
        assertSame(syms.findString("yumyum"), result);
    }
    @Test
    public void testCanParseAnIntegerConstant()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final Lexeme lexeme = new Lexeme();
        lexeme.type = LexemeType.INTEGER;
        lexeme.int_val = 456;
        
        final SymbolImpl result = DefaultSemanticMemory.smem_parse_constant_attr(syms, lexeme);
        assertNotNull(result);
        assertSame(syms.findInteger(456), result);
    }
    @Test
    public void testCanParseADoubleConstant()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final Lexeme lexeme = new Lexeme();
        lexeme.type = LexemeType.FLOAT;
        lexeme.float_val = 3.14159;
        
        final SymbolImpl result = DefaultSemanticMemory.smem_parse_constant_attr(syms, lexeme);
        assertNotNull(result);
        assertSame(syms.findDouble(3.14159), result);
    }
    
    @Test
    public void testCanParseAChunk() throws Exception
    {
        final DefaultSemanticMemory smem = new DefaultSemanticMemory(context);
        smem.initialize();
        
        smem.smem_parse_chunks("{" +
        		"(<arithmetic> ^add10-facts <a01> <a02> <a03>)\r\n" + 
        		"(<a01> ^digit1 1 ^digit-10 11)\r\n" + 
        		"(<a02> ^digit1 2 ^digit-10 12)\r\n" + 
        		"(<a03> ^digit1 3 ^digit-10 13)" +
        		"}");
        
        // TODO SMEM validate smem_parse_chunks
        
        final PrintWriter pw = new PrintWriter(System.out);
        smem.smem_visualize_store(pw);
        pw.flush();
    }
}
