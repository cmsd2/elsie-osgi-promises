package uk.org.elsie.osgi.promises;

public interface Canceller {
	public boolean cancel(boolean mayInterruptIfRunning);
}
