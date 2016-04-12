
public class Borrower {
	private int cardNo;
	private String borrowerName;
	private String borrowerAddress;
	private String borrowerPhone;
	/**
	 * @param cardNo
	 * @param borrowerName
	 * @param borrowerAddress
	 * @param borrowerPhone
	 */
	public Borrower(int cardNo, String borrowerName, String borrowerAddress,
			String borrowerPhone) {
		super();
		this.cardNo = cardNo;
		this.borrowerName = borrowerName;
		this.borrowerAddress = borrowerAddress;
		this.borrowerPhone = borrowerPhone;
	}
	public int getCardNo() {
		return cardNo;
	}
	public void setCardNo(int cardNo) {
		this.cardNo = cardNo;
	}
	public String getBorrowerName() {
		return borrowerName;
	}
	public void setBorrowerName(String borrowerName) {
		this.borrowerName = borrowerName;
	}
	public String getBorrowerAddress() {
		return borrowerAddress;
	}
	public void setBorrowerAddress(String borrowerAddress) {
		this.borrowerAddress = borrowerAddress;
	}
	public String getBorrowerPhone() {
		return borrowerPhone;
	}
	public void setBorrowerPhone(String borrowerPhone) {
		this.borrowerPhone = borrowerPhone;
	}
	
	
}
