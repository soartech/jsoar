package org.jsoar.kernel.memory;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SlotTest {

    @Test
    public void findSlotIdNull() {
        // Given identifier with value null
        IdentifierImpl id = null;

        // When looking for existing slot for id/attr pair
        Slot foundSlot = Slot.find_slot(id, mock(Symbol.class));

        // Then found slot is null
        assertNull(foundSlot);
    }

    @Test
    public void findSlotIdNoSlots() {
        // Given identifier with value null
        IdentifierImpl id = mock(IdentifierImpl.class);
        id.slots=null;

        // When looking for existing slot for id/attr pair
        Slot foundSlot = Slot.find_slot(id, mock(Symbol.class));

        // Then found slot is null
        assertNull(foundSlot);
    }

    @Test
    public void findSlotIdNoMatchingSlot() {
        // Given identifier with value null
        IdentifierImpl id = mock(IdentifierImpl.class);

        Slot slot = mock(Slot.class);
        slot.next=null;
        when(slot.getAttr()).thenReturn(null);

        id.slots=slot;

        // When looking for existing slot for id/attr pair
        Slot foundSlot = Slot.find_slot(id, mock(Symbol.class));

        // Then found slot is null
        assertNull(foundSlot);
    }

    @Test
    public void findSlotIdMatchingSlot() {
        // Given identifier with value null
        IdentifierImpl id = mock(IdentifierImpl.class);

        SymbolImpl attr = mock(SymbolImpl.class);

        Slot slot = mock(Slot.class);
        slot.next=null;
        when(slot.getAttr()).thenReturn(attr);

        id.slots=slot;


        // When looking for existing slot for id/attr pair
        Slot foundSlot = Slot.find_slot(id, attr);

        // Then found slot is null
        assertEquals(slot, foundSlot);
    }

}
