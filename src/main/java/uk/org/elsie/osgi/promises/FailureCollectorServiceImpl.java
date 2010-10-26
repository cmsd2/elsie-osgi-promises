package uk.org.elsie.osgi.promises;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A failure logger which prints out failures just before they're garbage collected
 * unless they've been collected by a callback.
 * @author chris
 */
public class FailureCollectorServiceImpl implements Callable<Object>, Canceller, FailureCollectorService {
	private static Log log = LogFactory.getLog(FailureCollectorServiceImpl.class);
	private ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();
	private Map<Reference<Object>, String> messages = new HashMap<Reference<Object>, String>();
	private ScheduledExecutorService executorService;
	private long delay = 5;
	private TimeUnit units = TimeUnit.SECONDS;
	private boolean finished = false;
	private ScheduledFuture<Object> future = null;
	private int maxErrors = 2;
	private int quota;
	private int maxPerSecond = 1;
	private int hardMaxErrors = 20;

	public FailureCollectorServiceImpl() {
	}
	
	public ScheduledExecutorService getScheduledExecutorService() {
		return executorService;
	}
	
	public synchronized void setScheduledExecutorService(ScheduledExecutorService executorService) {
		log.info("Set executor-service");
		this.executorService = executorService;
		start();
	}
	
	public synchronized void unsetScheduledExecutorService(ScheduledExecutorService executorService) {
		if(this.executorService == executorService) {
			log.info("Unset executor-service");
			shutdown();
			this.executorService = null;
		}
	}
	
	public long getDelay() {
		return delay;
	}
	
	public void setDelay(long delay) {
		this.delay = delay;
	}
	
	public TimeUnit getDelayUnit() {
		return units;
	}
	
	public void setDelayUnits(TimeUnit units) {
		this.units = units;
	}
	
	public synchronized void start() {
		if(future == null) {
			log.info("Starting failure collector");
			finished = false;
			schedule(delay, units);
		}
	}
	
	public synchronized void shutdown() {
		cancel(true);
	}
	
	public int getMaxErrors() {
		return maxErrors;
	}
	
	public void setMaxErrors(int maxErrors) {
		this.maxErrors = maxErrors;
	}

	@Override
	public synchronized Reference<Object> failed(Object failure) {
		if(hardMaxErrors > 0 && messages.size() >= hardMaxErrors) {
			return null;
		}

		String msg = failureObjToString(failure);
		PhantomReference<Object> ref = new PhantomReference<Object>(failure, refQueue);
		messages.put(ref, msg);
		return ref;
	}

	@Override
	public synchronized void collected(Reference<Object> ref) {
		messages.remove(ref);
	}
	
	public String failureObjToString(Object obj) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		pw.print(new Date().toString());
		pw.print(": ");

		if(obj instanceof Throwable) {
			appendThrowableString(pw, (Throwable) obj);
		} else if(obj instanceof Failure) {
			appendFailureString(pw, (Failure) obj);
		} else {
			pw.println(obj.toString());
		}
		
		return sw.toString();
	}
	
	public void appendFailureString(PrintWriter pw, Failure failure) {
		if(failure.getMessage() instanceof Throwable) {
			appendThrowableString(pw, (Throwable) failure.getMessage());
		} else {
			pw.println(failure.toString());
		}
		
		if(failure.getCause() != null) {
			appendThrowableString(pw, failure.getCause());
		}
	}
	
	public void appendThrowableString(PrintWriter pw, Throwable t) {
		t.printStackTrace(pw);
	}

	@Override
	public synchronized Object call() {
		boolean needsSchedule = true;
		try {
			quota = maxErrors;
			Reference<? extends Object> ref = refQueue.poll();
			while(ref != null) {
				if(quota > 0)
					quota--;
				String msg = messages.remove(ref);
				if(msg != null)
					log.error("Uncollected failure: " + msg);
				if(quota == 0) {
					schedule((long)(1000.0 * (1.0 / maxPerSecond)), TimeUnit.MILLISECONDS);
					needsSchedule = false;
					ref = null;
				} else if(finished) {
					ref = null;
				} else {
					ref = refQueue.poll();
				}
			}
		} catch (Exception e) {
			log.error("Error logging uncollected failure", e);
		}
		if(needsSchedule)
			schedule(delay, units);
		return null;
	}
	
	private synchronized void schedule(long delay, TimeUnit units) {
		if(!finished) {
			log.info("Rescheduling failure collector for " + delay + " " + units);
			future = executorService.schedule(this, delay, units);
		} else {
			future = null;
		}
	}

	@Override
	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		finished = true;
		if(future != null) {
			log.info("Stopping failure collector");
			boolean ret = future.cancel(mayInterruptIfRunning);
			future = null;
			return ret;
		} else {
			return false;
		}
	}
}
