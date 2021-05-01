package org.jsoar.kernel.memory;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

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
}
