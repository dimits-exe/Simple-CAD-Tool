package application.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import localisation.Languages;
import myUtil.StringGenerator;
import myUtil.Utility;

/**
 * Map wrapper for String-T pairs that additionally supports automatic ID
 * generation using {@link myUtil.StringGenerator StringGenerators}.
 *
 * @param <T> the type of the values stored
 *
 * @author alexm
 */
class ItemManager<T extends components.Identifiable<String>> {

	private final Map<String, T> map;
	private final Map<String, StringGenerator> generators;

	/** Constructs the ItemManager */
	ItemManager() {
		map = new HashMap<>();
		generators = new HashMap<>();
	}

	/**
	 * Adds an {@code Item} to the Manager.
	 *
	 * @param item the Item
	 */
	void add(T item) {
		String id = item.getID();
		if (map.containsKey(id))
			throw new DuplicateIdException(id);

		map.put(id, item);
	}

	/**
	 * Removes an {@code Item} from the Manager.
	 *
	 * @param item the Item
	 */
	void remove(T item) {
		map.remove(item.getID());
	}

	/**
	 * Returns the {@code Item} with the given ID.
	 *
	 * @param id the ID
	 *
	 * @return the Item with that ID
	 *
	 * @throws MissingComponentException if no Item with the ID exists
	 */
	T get(String id) throws MissingComponentException {
		final T item = map.get(id);
		if (item == null)
			throw new MissingComponentException(id);

		return item;
	}

	/**
	 * Returns the number of {@code Items} in this manager.
	 *
	 * @return the number of items
	 */
	int size() {
		return map.size();
	}

	/**
	 * Returns every {@code Item}.
	 * <p>
	 * <b>Note</b> that this does <i>not</i> return a copy of the items. Any changes
	 * to the Items will be reflected in this ItemManager object.
	 *
	 * @return a List with the Items
	 */
	List<T> getall() {
		return getall(c -> true);
	}

	/**
	 * Returns the Items for which the {@code predicate} evaluates to {@code true}.
	 * <p>
	 * <b>Note</b> that this does <i>not</i> return a copy of the items. Any changes
	 * to the Items will be reflected in this ItemManager object.
	 *
	 * @param predicate the Predicate that will be evaluated on each Item
	 *
	 * @return a List with the Items
	 */
	List<T> getall(Predicate<T> predicate) {
		List<T> list = new ArrayList<>(size());
		Utility.foreach(map.values(), item -> {
			if (predicate.test(item))
				list.add(item);
		});
		return list;
	}

	/**
	 * Returns the next ID generated by the specified Generator.
	 *
	 * @param genID the target Generator
	 *
	 * @return the next ID
	 */
	String getNextID(String genID) {
		return generators.get(genID).get();
	}

	/**
	 * Creates a Generator with the ID and text.
	 *
	 * @param genID the generator's ID
	 * @param text  the text that will be formatted
	 */
	void addGenerator(String genID, String text) {
		addGenerator(genID, text, 0);
	}

	/**
	 * Creates a generator with the ID, text and starting number.
	 *
	 * @param genid the generator's ID
	 * @param text  the text that will be formatted
	 * @param start the initial counter value
	 */
	void addGenerator(String genid, String text, int start) {
		generators.put(genid, new StringGenerator(text, start));
	}

	/**
	 * Thrown when another Item is already associated with the {@code ID}.
	 *
	 * @author alexm
	 */
	public static class DuplicateIdException extends RuntimeException {

		/**
		 * Constructs the Exception with information about the {@code ID}.
		 *
		 * @param id the duplicate id
		 */
		public DuplicateIdException(String id) {
			super(String.format("Another Item associated with ID %s", id)); 
		}
	}
}
