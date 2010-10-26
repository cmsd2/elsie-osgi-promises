package uk.org.elsie.osgi.promises;



public class CallbackFailure implements Failure {
	private Object message;
	private Throwable proximateCause;

	public CallbackFailure() {
	}
	
	public CallbackFailure(Object message) {
		this.message = message;
	}
	
	public CallbackFailure(Exception e) {
		this.proximateCause = e;
	}
	
	public CallbackFailure(Object message, Throwable proximateCause) {
		this.message = message;
		this.proximateCause = proximateCause;
	}
	
	public Object getMessage() {
		return message;
	}
	
	public Throwable getCause() {
		return proximateCause;
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("callback failed");
		
		if(message != null) {
			b.append(": ");
			b.append(message);
		}
		
		if(proximateCause != null) {
			b.append(": ");
			b.append(proximateCause);
		}
		
		return b.toString();
	}
}
