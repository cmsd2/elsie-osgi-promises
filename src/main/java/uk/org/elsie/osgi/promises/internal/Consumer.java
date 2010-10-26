package uk.org.elsie.osgi.promises.internal;

public interface Consumer<T> {
	public void call(T obj);
}
