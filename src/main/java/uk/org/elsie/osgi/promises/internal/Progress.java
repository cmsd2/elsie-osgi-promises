package uk.org.elsie.osgi.promises.internal;

public class Progress {
	private int completed;
	private int total;
	
	public Progress(int completed, int total) {
		this.completed = completed;
		this.total = total;
	}
	
	public int getCompleted() {
		return completed;
	}
	
	public int getTotal() {
		return total;
	}
}
