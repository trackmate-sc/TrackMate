package fiji.plugin.trackmate.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.features.FeatureAnalyzer;

public abstract class AbstractFeatureAnalyzerProvider<K extends FeatureAnalyzer> {


	/**
	 * The detector names, in the order they will appear in the GUI. These names
	 * will be used as keys to access relevant analyzer classes.
	 */
	protected List<String> names = new ArrayList<String>();

	protected Map<String, K> analyzers = new HashMap<String, K>();

	/**
	 * Registers the specified {@link FeatureAnalyzer} with the specified key.
	 * The analyzer is appended after all previously registered analyzers.
	 *
	 * @param key
	 *            a String used as a key for the analyzer.
	 * @param featureAnalyzer
	 *            the {@link FeatureAnalyzer} to register.
	 */
	protected void registerAnalyzer(final String key, final K featureAnalyzer) {
		names.add(key);
		analyzers.put(key, featureAnalyzer);
	}

	/**
	 * Registers the specified {@link FeatureAnalyzer} and inserts it at the
	 * specified position in the list of registered analyzers.
	 *
	 * @param index
	 *            the index to insert the analyzer.
	 * @param key
	 *            a String used as a key for the analyzer.
	 * @param featureAnalyzer
	 *            the {@link FeatureAnalyzer} to register.
	 */
	protected void registerAnalyzer(final int index, final String key, final K featureAnalyzer) {
		names.add(index, key);
		analyzers.put(key, featureAnalyzer);
	}

	/**
	 * Returns the instance of the target analyzer identified by the key
	 * parameter. If the key is unknown to this provider, <code>null</code> is
	 * returned.
	 */
	public K getFeatureAnalyzer(final String key) {
		return analyzers.get(key);
	}

	/**
	 * Returns a list of the analyzers keys available through this provider, in
	 * the order they were registered.
	 */
	public List<String> getAvailableFeatureAnalyzers() {
		return names;
	}
}
