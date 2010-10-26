package uk.org.elsie.osgi.promises;

import java.util.concurrent.Future;


public interface Deferred extends Future<Object>, Canceller {

	/**
	 * Sets the state to successful and the result to value.
	 * @param value the object to pass on as the success result.
	 */
	public abstract void resolve(Object value);

	/**
	 * Sets the state to failed and the result to reason.
	 * @param reason the object to pass on as the failure result.
	 */
	public abstract void reject(Object reason);

	/**
	 * Sets the nested canceller to use to abort execution.
	 * If null, or not set, the cancel method will return false.
	 * If set, the cancel method will call the canceller method
	 * then reject.
	 * @param canceller the canceller callback.
	 */
	public abstract void setCanceller(Canceller canceller);

	/**
	 * Gets a promise token which can be published and used
	 * for synchronisation or chaining work.
	 * @return the Promise.
	 */
	public abstract Promise getPromise();

	/**
	 * Task cancellation is successful when the nested canceller
	 * returns true.
	 * @return True if the task has been successfully cancelled. False otherwise.
	 */
	public abstract boolean isCancelled();

	/**
	 * Task completion happens when the task is resolved, rejected
	 * or cancelled successfully.
	 * @return True if the task has completed. False otherwise.
	 */
	public abstract boolean isDone();

}