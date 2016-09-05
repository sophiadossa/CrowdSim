package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class Processor<K extends Comparable<K>, V> {
	private int id;

	private String[] headers;
	private Map<K, V> data;

	private int lastStep;

	protected Processor() {
		this(new String[] { });
	}

	protected Processor(final String... headers) {
		this.headers = headers;
		this.data = new TreeMap<>(); // TreeMap to avoid sorting data later

		this.lastStep = 0;
	}

	protected Map<K, V> getData() {
		return this.data;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String[] getHeaders() {
		return this.headers;
	}

	public void setHeader(final String header) {
		this.headers = new String[] { header };
	}

	public Set<K> getKeys() {
		return this.getData().keySet();
	}

	public Collection<V> getValues() {
		return this.getData().values();
	}

	public boolean hasValue(final K key) {
		return this.data.containsKey(key);
	}

	public V getValue(final K key) {
		return data.get(key);
	}

	protected void addValue(final K key, final V value) {
		this.data.put(key, value);
	}

	public void preLoop(final SimulationState state) { }

	protected abstract void doUpdate(final SimulationState state);

	public final void update(final SimulationState state) {
		int step = state.getStep();

		if (this.lastStep < step) {
			this.doUpdate(state);
			this.lastStep = step;
		}
	}

	public void postLoop(final SimulationState state) { }

	public abstract void init(final AttributesProcessor attributes, final ProcessorManager manager);

	public String[] toStrings(final K key) {
		return new String[] { this.hasValue(key) ? this.getValue(key).toString() : "NaN" };
	}
}
