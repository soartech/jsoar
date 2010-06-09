package org.jsoar.kernel.memory;

import java.util.Formatter;
import java.util.Iterator;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

public class DummyWme implements Wme {
	
	final private Identifier id;
	final private Symbol attr;
	final private Symbol value;

	public DummyWme(Identifier id, Symbol attr, Symbol value) {
		super();
		this.id = id;
		this.attr = attr;
		this.value = value;
	}

	@Override
	public Symbol getAttribute() {
		// TODO Auto-generated method stub
		return attr;
	}

	@Override
	public Iterator<Wme> getChildren() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Identifier getIdentifier() {
		// TODO Auto-generated method stub
		return id;
	}

	@Override
	public Iterator<Preference> getPreferences() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTimetag() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Symbol getValue() {
		// TODO Auto-generated method stub
		return value;
	}

	@Override
	public boolean isAcceptable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width,
			int precision) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getAdapter(Class<?> klass) {
		// TODO Auto-generated method stub
		return null;
	}
	
}