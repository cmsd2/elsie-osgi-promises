package uk.org.elsie.osgi.promises;

public interface Failure {
	public Object getMessage();
	public Throwable getCause();
}
