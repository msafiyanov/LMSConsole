
public class Publisher {
	private int pubId;
	private String pubName;
	private String pubAddress;
	private String pubPhone;
	public Publisher(int pubId, String pubName, String pubAddress,
			String pubPhone) {
		this.pubId = pubId;
		this.pubName = pubName;
		this.pubAddress = pubAddress;
		this.pubPhone = pubPhone;
	}
	public int getPubId() {
		return pubId;
	}
	public void setPubId(int pubId) {
		this.pubId = pubId;
	}
	public String getPubName() {
		return pubName;
	}
	public void setPubName(String pubName) {
		this.pubName = pubName;
	}
	public String getPubAddress() {
		return pubAddress;
	}
	public void setPubAddress(String pubAddress) {
		this.pubAddress = pubAddress;
	}
	public String getPubPhone() {
		return pubPhone;
	}
	public void setPubPhone(String pubPhone) {
		this.pubPhone = pubPhone;
	}
	

}
