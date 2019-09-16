package gdou.laixiaoming.springcloudstreambinderactivemq2.properties;

public class ActiveMQConsumerProperties{

	private String destination;
	
	private String partition;
	
	private boolean transaction;

	//autoDlq
	private boolean republishToDlq;
	
	public boolean isTransaction() {
		return transaction;
	}

	public void setTransaction(boolean transaction) {
		this.transaction = transaction;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getPartition() {
		return partition;
	}

	public void setPartition(String partition) {
		this.partition = partition;
	}

	public boolean isRepublishToDlq() {
		return republishToDlq;
	}

	public void setRepublishToDlq(boolean republishToDlq) {
		this.republishToDlq = republishToDlq;
	}
}
