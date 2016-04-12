/**
 * 
 */

/**
 * @author Meirbek
 *
 */
public class LibraryBranch {
	private String branchName;
	private String branchAddress;
	private int branchId;
	
	/**
	 * @param branchName represents the name of the Library Branch
	 * @param branchAddress represents the address of the Library Branch
	 */
	public LibraryBranch(int id, String branchName, String branchAddress) {
		this.branchName = branchName;
		this.branchAddress = branchAddress;
		this.branchId = id;
	}

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	public String getBranchAddress() {
		return branchAddress;
	}

	public void setBranchAddress(String branchAddress) {
		this.branchAddress = branchAddress;
	}

	public int getId() {
		return branchId;
	}

	public void setId(int id) {
		this.branchId = id;
	}
		
}
