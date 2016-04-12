import java.io.ObjectInputStream.GetField;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.RestoreAction;

/**
 * @author Meirbek
 * A LIbrary management application which lets Librarian, Admin or Borrower use the library system
 * 	to add/update/delete books, authors, piblishers, library branches, etc.
 */
public class LMS {
	
	static Scanner in;
	static Connection conn;
	
	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		//
		in = new Scanner(System.in);
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection("jdbc:mysql://localhost/library", "root","root");
		
		int user = 0;
		
		do {
			// Welcome message		
			welcomeMessage();			
			
			String st = in.next();
			if (st.length() > 1 ) break;
			
			user = Integer.parseInt(st);
			if (user == 1)
				librarianMenu();
			else if (user == 2)
				adminMenu();
			else if (user == 3)
				borrowerMenu();
			else
				System.err.println("Please select a number between 1 and 3");
			
		} while (user >= 1 && user <= 3);

	}
	
	private static void borrowerMenu() throws SQLException {
		int cardNo = 0;
		PreparedStatement pstmt;
		ResultSet rs;
		do {
			System.out.println();
			System.out.println("Enter the your Card Number:");
			cardNo = in.nextInt();
			
			pstmt = conn.prepareStatement("select cardNo from tbl_borrower where cardNo = ?");
			pstmt.setInt(1, cardNo);
			 rs = pstmt.executeQuery();
			if (rs.first())	break;
			System.err.println("Your Card Number is invalid!");
		} while (true);
		
		int menu = 0;
		List<LibraryBranch> libraryBranches; // list of library Branches
		Map<Book, List<Author>> books; // Map containing a book as a key and a list of authors as value
		
		while (true) {
			// 
			System.out.println();
			System.out.println("1)  Check out a book");
			System.out.println("2)  Return a Book");
			System.out.println("3)  Quit to previous");
			System.out.println();
			
			menu = in.nextInt();
			if (menu < 1 || menu > 3) {
				System.err.println(" Please enter 1, 2 or 3:");
			}
			else if (menu == 3) return;
						
			else if (menu == 1) {
				// List all the branches in the db
				libraryBranches = queryLibraryBranch();
				System.out.println("Pick the Branch you want to check out from:");
				int chosenLibraryBranch = printAndChooseLibraryBranch(libraryBranches);
				if (chosenLibraryBranch == libraryBranches.size() + 1) continue;
				int libId = libraryBranches.get(chosenLibraryBranch - 1).getId();
				
				System.out.println();
				System.out.println("Pick the Book you want to check out");
				String query = "select b.bookId, b.title, b.pubId, a.authorId, a.authorName, bc.noOfCopies "
							 + " from tbl_book as b "
							 + " left join tbl_book_authors as ba " 
							 + " on b.bookId = ba.bookId "
							 + " left join tbl_author as a "
							 + " on ba.authorId = a.authorId "
							 + " join tbl_book_copies as bc "
							 + " on b.bookId = bc.bookId "
							 + " where bc.noOfCopies > 0 and bc.branchID = " 
							 + Integer.toString(libId) + ";";
				books = selectBooks(query);
				printBooks(books);
				
				do {
					menu = in.nextInt();
				} while (menu < 1 || menu > books.size() + 1); 
				
				if (menu <= books.size())
				{
					for (Book book : books.keySet()) {
						if (book.getBookListNumber() == menu) //if selected book = enumerated book
						{
							loanBook(libraryBranches.get(chosenLibraryBranch - 1), book, cardNo);
							break;
						}
					}
				}
			}
			else if (menu == 2) {
				libraryBranches = queryLibraryBranch();
				System.out.println("Pick the Branch you want to return book to:");
				int chosenLibraryBranch = printAndChooseLibraryBranch(libraryBranches);
				if (chosenLibraryBranch == libraryBranches.size() + 1) continue;
				int libId = libraryBranches.get(chosenLibraryBranch - 1).getId();
				
				System.out.println();
				System.out.println("Pick the Book you want to return");
				
				String query = "select b.bookId, b.title, b.pubId, a.authorId, a.authorName, bc.noOfCopies " 
							 + " from tbl_book as b "
							 + " left join tbl_book_authors as ba "
							 + " on b.bookId = ba.bookId "
							 + " left join tbl_author as a "
							 + " on ba.authorId = a.authorId "
							 + " join tbl_book_copies as bc "
							 + " on b.bookId = bc.bookId "
							 + " where bc.noOfCopies IS NOT NULL and bc.branchID = " 
							 + Integer.toString(libId) +";";
				books = selectBooks(query);
				printBooks(books);
				
				do {
					menu = in.nextInt();
				} while (menu < 1 || menu > books.size() + 1); 
				
				if (menu <= books.size())
				{
					for (Book book : books.keySet()) {
						if (book.getBookListNumber() == menu) //if selected book = enumerated book
						{
							returnBook(libraryBranches.get(chosenLibraryBranch - 1), book, cardNo);
							break;
						}
					}
				}
			}
		}		
	}

	private static void returnBook(LibraryBranch libraryBranch, Book book,
			int cardNo) throws SQLException {
		// Check if borrower have already checked out the same book from this library
		PreparedStatement pstmt = conn.prepareStatement("select bookId from tbl_book_loans "
													 + " where bookId = ? and branchId = ? and cardNo = ? and dateIn IS NULL");
		pstmt.setInt(1, book.getBookId());
		pstmt.setInt(2, libraryBranch.getId());
		pstmt.setInt(3, cardNo);
				
		ResultSet rs = pstmt.executeQuery();
		if (!rs.next())
		{
			System.err.println("You have not yet checked out this book from this library. Nothing to return");
			return;
		}
		rs.beforeFirst();
		
		pstmt = conn.prepareStatement("update tbl_book_loans set dateIn = ? where bookId = ? and branchId = ? and cardNo = ?");
		pstmt.setDate(1, getCurrentDate());
		pstmt.setInt(2, book.getBookId());
		pstmt.setInt(3, libraryBranch.getId());
		pstmt.setInt(4, cardNo);
		pstmt.executeUpdate();
				
		// adding 1 to noOfCopies in tbl_book_copies table
		pstmt = conn.prepareStatement("update tbl_book_copies set noOfCopies = noOfCopies + 1 where bookId = ? and branchId = ?");
		pstmt.setInt(1, book.getBookId());
		pstmt.setInt(2, libraryBranch.getId());
		pstmt.executeUpdate();
		
		System.out.println("THe book is successfully returned");
		return;
		
	}

	private static void loanBook(LibraryBranch libraryBranch, Book book,
			int cardNo) throws SQLException {
		
		// Check if borrower have already checked out the same book from this library
		PreparedStatement pstmt = conn.prepareStatement("select bookId, dateIn from tbl_book_loans "
													 + " where bookId = ? and branchId = ? and cardNo = ?");
		pstmt.setInt(1, book.getBookId());
		pstmt.setInt(2, libraryBranch.getId());
		pstmt.setInt(3, cardNo);
		
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()) // Check if the book has already been checked out from this library branch
		{
			Date d = rs.getDate("dateIn");
			if (d == null)
			{
				System.err.println("You have already checked out the same book from this library. You cannot check another copy out");
				return;
			}
			// If the book has been returned, then we can check it out again. So delete the record for now. We will create the new record in the future
			PreparedStatement deletePstmt = conn.prepareStatement("delete from tbl_book_loans where bookId=? and branchId=? and cardNo=?");
			deletePstmt.setInt(1, book.getBookId());
			deletePstmt.setInt(2, libraryBranch.getId());
			deletePstmt.setInt(3, cardNo);
			deletePstmt.executeUpdate();
		}
		
		rs.beforeFirst();
		
		// Creating a record in book_loans table to check out the book
		pstmt = conn.prepareStatement("insert into tbl_book_loans (bookId, branchId, cardNo, dateOut, dueDate, dateIn) values (?,?,?,?,?,null)");
		pstmt.setInt(1, book.getBookId());
		pstmt.setInt(2, libraryBranch.getId());
		pstmt.setInt(3, cardNo);
		pstmt.setDate(4, getCurrentDate());
		pstmt.setDate(5, getWeekFromDate());
		pstmt.executeUpdate();
				
		// subtracting 1 from noOfCopies in tbl_book_copies table
		pstmt = conn.prepareStatement("update tbl_book_copies set noOfCopies = noOfCopies - 1 where bookId = ? and branchId = ?");
		pstmt.setInt(1, book.getBookId());
		pstmt.setInt(2, libraryBranch.getId());
		pstmt.executeUpdate();
		
		System.out.println();
		System.out.println("The book successfully checked out");
		System.out.println();
	}

	private static java.sql.Date getCurrentDate() {		
	    return java.sql.Date.valueOf(java.time.LocalDate.now());
	}
	
	private static java.sql.Date getWeekFromDate() {
		return java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(7));	
	}

	private static void adminMenu() throws SQLException {
		
		int menu = 0;
		
		while (true) {
			// 
			System.out.println();
			System.out.println("1)  Add/Update/Delete Book and Author");
			System.out.println("2)  Add/Update/Delete Publishers");
			System.out.println("3)  Add/Update/Delete Library Branches");
			System.out.println("4)  Add/Update/Delete Borrowers");
			System.out.println("5)  Over-ride Due Date for a Book Loan");
			System.out.println("6)  Quit to previous");
			System.out.println();
			
			menu = in.nextInt();
			if (menu < 1 || menu > 6) {
				System.err.println(" Please enter 1, 2, 3, 4, 5 or 6:");
			}
			else if (menu == 6)
				return;
			else if (menu == 1)
			{
				manageBookAndAuthors();
			}
			else if (menu == 2)
			{
				managePublisher();
			}
			else if (menu == 3)
			{
				manageLibraryBranch();
			}
			else if (menu == 4)
			{
				manageBorrower();
			}
			else if (menu == 5)
			{
				newDueDateOfBookLoan();
			}
			
		}
	}

	
	private static void manageLibraryBranch() throws SQLException {
		int menu = 0;
		PreparedStatement pstmt;
		
		while (true) {
			// 
			System.out.println();
			System.out.println("1)  Add Library Branch");
			System.out.println("2)  Update Library Branch");
			System.out.println("3)  Delete Library Branch");
			System.out.println("4)  Quit to previous");
			System.out.println();
			
			menu = in.nextInt();
			
			if (menu == 1) { // Adding a Library Branch			
				System.out.println();
				System.out.println("Please enter the name of new Library Branch");
				System.out.println();
				String newLibraryBranchName = in.nextLine();
				newLibraryBranchName = in.nextLine();
				System.out.println();
				System.out.println("Please enter the address of the new Library Branch");
				System.out.println();
				String newLibraryBranchAddress = in.nextLine();
				
				pstmt = conn.prepareStatement("insert into tbl_library_branch (branchName, branchAddress) values (?,?)");
				pstmt.setString(1, newLibraryBranchName);
				pstmt.setString(2, newLibraryBranchAddress);
				
				if (pstmt.executeUpdate() == 1)
				{
					System.out.println();
					System.out.println("Library Branch created successfully");
					System.out.println();
				}
			}
			
			else if (menu == 2 || menu == 3) { // Updating or Deleting Library Branch
				pstmt = conn.prepareStatement("select branchId, branchName, branchAddress from tbl_library_branch");
				ResultSet rs = pstmt.executeQuery();
				
				int branchId = 0;
				String branchName, branchAddress;
				List<LibraryBranch> libraryBranches = new ArrayList<LibraryBranch>();
				int i = 0;
				
				while (rs.next()) {
					i++;
					branchId = rs.getInt(1);
					branchName = rs.getString(2);
					branchAddress = rs.getString(3);
					LibraryBranch p = new LibraryBranch(branchId, branchName, branchAddress);
					libraryBranches.add(p);
					System.out.println(i + ")  " + branchName + ", " + branchAddress);
				}
				
				System.out.println(libraryBranches.size() + 1 + ")  Quit to previous");
								
				int updateDeleteBranch = in.nextInt();
				
				if (libraryBranches.size() == 0) 
				{
					System.out.println("There are no existing library branches. Nothing to upgrade/delete");
					continue;
				}
				if (updateDeleteBranch == libraryBranches.size() + 1)
					continue;
				
				if (menu == 2) { // Update a Library Branch
					System.out.println();
					System.out.println("Please enter new name of the Library Branch");
					System.out.println();
					String newBranchName = in.nextLine();
					newBranchName = in.nextLine();
					System.out.println();
					System.out.println("Please enter new address of the Library Branch");
					System.out.println();
					String newBranchAddress = in.nextLine();
					
					pstmt = conn.prepareStatement("update tbl_library_branch set branchName = ?, branchAddress = ? where branchId = ?");
					pstmt.setString(1, newBranchName);
					pstmt.setString(2, newBranchAddress);
					pstmt.setInt(3, libraryBranches.get(updateDeleteBranch - 1).getId());
					pstmt.executeUpdate();
					
					libraryBranches.get(updateDeleteBranch - 1).setBranchName(newBranchName);
					libraryBranches.get(updateDeleteBranch - 1).setBranchAddress(newBranchAddress);
					
					System.out.println();
					System.out.println("Library Branch updated successfully");
					System.out.println();
				}
				else { // Delete a Library Branch
					pstmt = conn.prepareStatement("delete from tbl_library_branch where branchId = ?");
					pstmt.setInt(1, libraryBranches.get(updateDeleteBranch - 1).getId());
					pstmt.executeUpdate();
					System.out.println();
					System.out.println("Library Branch deleted successfully");
					System.out.println();
				}
				
			}
			else if (menu == 4)
				return;
			else { 
				System.out.println();
				System.err.println(" Please enter 1, 2, 3 or 4:");
				System.out.println();
			}
		}		
	}

	private static void manageBorrower() throws SQLException {

		int menu = 0;
		PreparedStatement pstmt;
		
		while (true) {
			// 
			System.out.println();
			System.out.println("1)  Add Borrower");
			System.out.println("2)  Update Borrower");
			System.out.println("3)  Delete Borrower");
			System.out.println("4)  Quit to previous");
			System.out.println();
			
			menu = in.nextInt();
			
			if (menu == 1) { // Adding a Borrower			
				System.out.println();
				System.out.println("Please enter the name of new Borrower");
				System.out.println();
				String newBorrowerName = in.nextLine();
				newBorrowerName = in.nextLine();
				System.out.println();
				System.out.println("Please enter the address of new Borrower");
				System.out.println();
				String newBorrowerAddress = in.nextLine();
				System.out.println();
				System.out.println("Please enter the phone of new Borrower");
				System.out.println();
				String newBorrowerPhone = in.nextLine();
				
				pstmt = conn.prepareStatement("insert into tbl_borrower (name, address, phone) values (?,?,?)");
				pstmt.setString(1, newBorrowerName);
				pstmt.setString(2, newBorrowerAddress);
				pstmt.setString(3, newBorrowerPhone);
				
				if (pstmt.executeUpdate() == 1)
				{
					System.out.println();
					System.out.println("Borrower created successfully");
					System.out.println();
				}
			}
			
			else if (menu == 2 || menu == 3) { // Updating or Deleting Borrower
				pstmt = conn.prepareStatement("select cardNo, name, address, phone from tbl_borrower");
				ResultSet rs = pstmt.executeQuery();
				
				int cardNo = 0;
				String borName, borAddress, borPhone;
				List<Borrower> borrowers = new ArrayList<Borrower>();
				int i = 0;
				
				while (rs.next()) {
					i++;
					cardNo = rs.getInt(1);
					borName = rs.getString(2);
					borAddress = rs.getString(3);
					borPhone = rs.getString(4);	
					Borrower p = new Borrower(cardNo, borName, borAddress, borPhone);
					borrowers.add(p);
					System.out.println(i + ")  " + borName + ", " + borAddress + ", " + borPhone);
				}
				
				System.out.println(borrowers.size() + 1 + ")  Quit to previous");
								
				int updateDeleteBorrower = in.nextInt();
				
				if (borrowers.size() == 0) 
				{
					System.out.println("There are no existing borrowers. Nothing to upgrade/delete");
					continue;
				}
				if (updateDeleteBorrower == borrowers.size() + 1)
					continue;
			
				if (menu == 2) { // Update a Borrower
					System.out.println();
					System.out.println("Please enter new name of the Borrower");
					System.out.println();
					String newBorrowerName = in.nextLine();
					newBorrowerName = in.nextLine();
					System.out.println();
					System.out.println("Please enter new address of the Borrower");
					System.out.println();
					String newBorrowerAddress = in.nextLine();
					System.out.println();
					System.out.println("Please enter new phone of the Borrower");
					System.out.println();
					String newBorrowerPhone = in.nextLine();
					
					pstmt = conn.prepareStatement("update tbl_borrower set name = ?, address = ?, phone = ? where cardNo = ?");
					pstmt.setString(1, newBorrowerName);
					pstmt.setString(2, newBorrowerAddress);
					pstmt.setString(3, newBorrowerPhone);
					pstmt.setInt(4, borrowers.get(updateDeleteBorrower - 1).getCardNo());
					pstmt.executeUpdate();
					
					borrowers.get(updateDeleteBorrower - 1).setBorrowerName(newBorrowerName);
					borrowers.get(updateDeleteBorrower - 1).setBorrowerAddress(newBorrowerAddress);
					borrowers.get(updateDeleteBorrower - 1).setBorrowerPhone(newBorrowerPhone);
					
					System.out.println();
					System.out.println("Borrower updated successfully");
					System.out.println();
				}
				else { // Delete a Borrower
					pstmt = conn.prepareStatement("delete from tbl_borrower where cardNo = ?");
					pstmt.setInt(1, borrowers.get(updateDeleteBorrower - 1).getCardNo());
					pstmt.executeUpdate();
					System.out.println();
					System.out.println("Borrower deleted successfully");
					System.out.println();
				}
				
			}
			else if (menu == 4)
				return;
			else { 
				System.out.println();
				System.err.println(" Please enter 1, 2, 3 or 4:");
				System.out.println();
			}
		}		
	}

	private static void managePublisher() throws SQLException {
		int menu = 0;
		PreparedStatement pstmt;
		
		while (true) {
			// 
			System.out.println();
			System.out.println("1)  Add Publisher");
			System.out.println("2)  Update Publisher");
			System.out.println("3)  Delete Publisher");
			System.out.println("4)  Quit to previous");
			System.out.println();
			
			menu = in.nextInt();
			
			if (menu == 1) { // Adding a Publisher			
				System.out.println();
				System.out.println("Please enter the name of new Publisher");
				System.out.println();
				String newPublisherName = in.nextLine();
				newPublisherName = in.nextLine();
				System.out.println();
				System.out.println("Please enter the address of the new Publisher");
				System.out.println();
				String newPublisherAddress = in.nextLine();
				System.out.println();
				System.out.println("Please enter the phone of the new Publisher");
				System.out.println();
				String newPublisherPhone = in.nextLine();
				
				pstmt = conn.prepareStatement("insert into tbl_publisher (publisherName, publisherAddress, publisherPhone) values (?,?,?)");
				pstmt.setString(1, newPublisherName);
				pstmt.setString(2, newPublisherAddress);
				pstmt.setString(3, newPublisherPhone);
				
				if (pstmt.executeUpdate() == 1)
				{
					System.out.println();
					System.out.println("Publisher created successfully");
					System.out.println();
				}
			}
			
			else if (menu == 2 || menu == 3) { // Updating or Deleting Publisher
				pstmt = conn.prepareStatement("select publisherId, publisherName, publisherAddress, publisherPhone from tbl_publisher");
				ResultSet rs = pstmt.executeQuery();
				
				int pubId = 0;
				String pubName, pubAddress, pubPhone;
				List<Publisher> publishers = new ArrayList<Publisher>();
				int i = 0;
				
				while (rs.next()) {
					i++;
					pubId = rs.getInt(1);
					pubName = rs.getString(2);
					pubAddress = rs.getString(3);
					pubPhone = rs.getString(4);
					Publisher p = new Publisher(pubId, pubName, pubAddress, pubPhone);
					publishers.add(p);
					System.out.println(i + ")  " + pubName + ", " + pubAddress + ", " + pubPhone);
				}
				
				System.out.println(publishers.size() + 1 + ")  Quit to previous");
								
				int updateDeletePublisher = in.nextInt();
				
				if (publishers.size() == 0) 
					{
						System.out.println("There are no existing publishers. Nothing to upgrade/delete");
						continue;
					}
				if (updateDeletePublisher == publishers.size() + 1)
					continue;
				if (menu == 2) { // Update a publisher
					System.out.println();
					System.out.println("Please enter new name of the Publisher");
					System.out.println();
					String newPublisherName = in.nextLine();
					newPublisherName = in.nextLine();
					System.out.println();
					System.out.println("Please enter new address of the Publisher");
					System.out.println();
					String newPublisherAddress = in.nextLine();
					System.out.println();
					System.out.println("Please enter new phone of the Publisher");
					System.out.println();
					String newPublisherPhone = in.nextLine();
					
					pstmt = conn.prepareStatement("update tbl_publisher set publisherName = ?, publisherAddress = ?, publisherPhone = ? where publisherId = ?");
					pstmt.setString(1, newPublisherName);
					pstmt.setString(2, newPublisherAddress);
					pstmt.setString(3, newPublisherPhone);
					pstmt.setInt(4, publishers.get(updateDeletePublisher - 1).getPubId());
					pstmt.executeUpdate();
					
					publishers.get(updateDeletePublisher - 1).setPubName(newPublisherName);
					publishers.get(updateDeletePublisher - 1).setPubAddress(newPublisherAddress);
					publishers.get(updateDeletePublisher - 1).setPubPhone(newPublisherPhone);
					
					System.out.println();
					System.out.println("Publisher updated successfully");
					System.out.println();
				}
				else { // Delete a publisher
					pstmt = conn.prepareStatement("delete from tbl_publisher where publisherId = ?");
					pstmt.setInt(1, publishers.get(updateDeletePublisher - 1).getPubId());
					pstmt.executeUpdate();
					System.out.println();
					System.out.println("Publisher deleted successfully");
					System.out.println();
				}
				
			}
			else if (menu == 4)
				return;
			else { 
				System.out.println();
				System.err.println(" Please enter 1, 2, 3 or 4:");
				System.out.println();
			}
		}
	}

	private static void newDueDateOfBookLoan() throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("select bookId, branchId, cardNo, dueDate, dateOut, dateIn from tbl_book_loans");
		ResultSet rs = pstmt.executeQuery();
		List<BookLoan> bookLoans = new ArrayList<BookLoan>();
		
		System.out.println();
		System.out.println("Here is the list of all book loans in this format - (bookId, branchId, cardNo, dueDate");
		while (rs.next())
		{
			int bookId = rs.getInt(1);
			int branchId = rs.getInt(2);
			int cardNo = rs.getInt(3);
			Date dueDate= rs.getDate(4);
			Date dateOut = rs.getDate(5);
			Date dateIn = rs.getDate(6);
			BookLoan bl = new BookLoan(bookId, branchId, cardNo, dateOut, dueDate, dateIn);
			bookLoans.add(bl);
			System.out.println(bookId + "    " + branchId + "    " + cardNo + "    " + dueDate);
		}
		System.out.println();
		System.out.println("Please select a record by entering a combination of bookId, branchId and cardNo separated by empty space.");
		System.out.println();
		int selectBookId = in.nextInt();
		int selectBranchId = in.nextInt();
		int selectCardNo = in.nextInt();
		System.out.println();
		System.out.println("Enter the number of days you want to extend the dueDate to");
		System.out.println();
		
		int addDays = in.nextInt();		
		
		BookLoan selectBL = new BookLoan(selectBookId, selectBranchId, selectCardNo);
		if (!bookLoans.contains(selectBL)) {
			System.err.println("The combination you have chosen is not in the list");
			System.err.println("Quitting");
			System.out.println();
			return;
		}
		BookLoan bl =  bookLoans.get(bookLoans.indexOf(selectBL));
		LocalDate dueDate = bl.getDueDate().toLocalDate();
	
		java.sql.Date sqlDate = java.sql.Date.valueOf(dueDate.plusDays(addDays));	
		
		pstmt = conn.prepareStatement("update tbl_book_loans set dueDate = ? where bookId = ? and branchId = ? and cardNo = ?");
		pstmt.setDate(1, sqlDate);
		pstmt.setInt(2, selectBookId);
		pstmt.setInt(3, selectBranchId);
		pstmt.setInt(4, selectCardNo);
		pstmt.executeUpdate();
		System.out.println();
		System.out.println("dueDate of book_loans have been upgraded");
		System.out.println();
	}

	private static void manageBookAndAuthors() throws SQLException {
		
		int menu = 0;
		
		while (true) {
			// 
			System.out.println();
			System.out.println("1)  Add Book and Author");
			System.out.println("2)  Update Book and Author");
			System.out.println("3)  Delete Book and Author");
			System.out.println("4)  Quit to previous");
			System.out.println();
			
			menu = in.nextInt();
			if (menu < 1 || menu > 4) {
				System.err.println(" Please enter 1, 2, 3 or 4:");
			}
			else if (menu == 4)
				return;
			
			else if (menu == 1) // Adding a new Book
			{
				addBook();
			}
			
			else {	// Updating or Deleting a book with authors
				
				// a list containing Books
				Map<Book, List<Author>> books = selectBooks("select b.bookId, b.title, b.pubId, a.authorId, a.authorName from tbl_book as b left join tbl_book_authors as ba on b.bookId = ba.bookId left join tbl_author as a on ba.authorId = a.authorId;");
				// Printing all existing Books			
				printBooks(books);
								
				// Choose a book, which you want to update or delete
				int updateDeleteMenu = 0;
				do {
					updateDeleteMenu = in.nextInt();
				} while (updateDeleteMenu < 1 || updateDeleteMenu > books.size() + 1); 
					
				if (updateDeleteMenu <= books.size())
				{
					for (Book book : books.keySet()) {
						if (book.getBookListNumber() == updateDeleteMenu) // if selected book = enumerated book
						{
							if (menu == 2) // Updating a Book with Authors
							{
								int updateMenu = 0;
								
								System.out.println();
								System.out.println("1)  Update Book ");
								System.out.println("2)  Update Author");
								System.out.println("3)  Quit to previous");
								System.out.println();
									
								updateMenu = in.nextInt();
								if (menu < 1 || menu > 3) {
									System.err.println(" Please enter 1, 2 or 3:");
								}
								else if (updateMenu == 3)
									return;
								else 
									updateBook(book, books.get(book), updateMenu);								
								
							}
							else if (menu == 3) // Deleting a Book with Authors
							{
								int updateMenu = 0;
								System.out.println();
								System.out.println("1)  Delete Book completely");
								System.out.println("2)  Delete Author from this book");
								System.out.println("3)  Quit to previous");
								System.out.println();
									
								updateMenu = in.nextInt();
								if (menu < 1 || menu > 3) {
									System.err.println(" Please enter 1, 2 or 3:");
									System.out.println();
								}
								else if (updateMenu == 3)
									return;
								else 
									deleteBook(book, books.get(book), updateMenu);									
							}							
							break;
						}
					}
				}				
			}			
		}			
	}

	/**
	 * A method that creates a new Book in the db.
	 * The book may be created with a publisher selected from publisher list from db.
	 * Any number of authors may be added to the book. 
	 * @throws SQLException
	 */
	private static void addBook() throws SQLException{
		String newBookTitle;
				
		System.out.println();
		System.out.println("Please create a Title for your new Book");
		System.out.println();
		
		newBookTitle = in.nextLine();
		newBookTitle = in.nextLine();
		
		addBookLinkPulisherToBook(newBookTitle);			
		
		while (true) {
			linkAuthorToBook(newBookTitle);
			
			System.out.println();
			System.out.println("1)  Add more authors to this book: " + newBookTitle);
			System.out.println("2)  Done. Quit to previous");
			System.out.println();
			int menu = in.nextInt();
			if (menu != 1) break;
		}
	}

	/**
	 * Links the book to an author - author may exist in db or can be created
	 * @param newBookTitle Title of the book
	 * @throws SQLException
	 */
	private static void linkAuthorToBook(String newBookTitle) throws SQLException {
		
		PreparedStatement pstmt;
		ResultSet rs;
		
		// Print out all the authors
		List<Author> authors = printAuthors();		
		
		// There is no author in the db. In other words, tbl_author is empty
		if (authors.size() == 0)
		{
			System.out.println();
			System.out.println("There are no authors in the TBL_AUTHOR table. Type 1 to create a new author.");
			System.out.println("Type any other number <> 0 to create a book without author");
			System.out.println();
		}
		
		// get the bookId of book
		pstmt = conn.prepareStatement("select bookId from tbl_book where title = ?" );
		pstmt.setString(1, newBookTitle);
		rs = pstmt.executeQuery();
		int bookId = rs.getInt("bookId");
		
		int authorId;
		String authorName;
				
		int menu = in.nextInt();
		
		if (menu == authors.size() + 1) // Creating new author
		{
			System.out.println("Enter the name of the new author");
			System.out.println();
			authorName = in.nextLine();
			authorName = in.nextLine();
			
			// create author 
			pstmt = conn.prepareStatement("insert into tbl_author (authorName) values (?)" );
			pstmt.setString(1, authorName);
			pstmt.executeUpdate();			
			
			// find the authorId of newly created author
			pstmt = conn.prepareStatement("select authorId from tbl_author where authorName = ?" );
			pstmt.setString(1, authorName);
			rs = pstmt.executeQuery();
			
			authorId = rs.getInt("authorId");			
		}
		else if (menu <= authors.size()) // Author from the database
		{
			while (rs.next())
				bookId = rs.getInt("bookId");
			
			authorId = authors.get(menu - 1).getAuthorID();			
		}
		else 
			{
				System.out.println("Invalid author selected. Quitting.");
				return;
			}
		
		// linking author with book in tbl_book_authors table
		pstmt = conn.prepareStatement("insert into tbl_book_authors (bookId, authorId) values (?,?)" );
		pstmt.setInt(1, bookId);
		pstmt.setInt(2, authorId);
		pstmt.executeUpdate();
	}

	private static void addBookLinkPulisherToBook(String newBookTitle) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("select publisherName, publisherId from tbl_publisher");
		ResultSet rs = pstmt.executeQuery();
		String newPublisherName, pubName;
		int pubId;
		
		System.out.println("The list of existing publishers:");
		while (rs.next())
		{
			pubName = rs.getString(1);
			pubId = rs.getInt(2);
			System.out.println(pubName);
		}
		System.out.println();
		System.out.println("Please enter the name of the publisher from the list above. Enter the actual name, NOT a number:");
		newPublisherName = in.nextLine();
		
		//Lookup the tbl_publisher to see if this Publisher is already in the table
		pstmt = conn.prepareStatement("select publisherName, publisherId from tbl_publisher");
		rs = pstmt.executeQuery();
		while (rs.next())
		{
			pubName = rs.getString(1);
			pubId = rs.getInt(2);
			if (newPublisherName.equals(pubName)) // Publisher found in the list, so publisher will be linked to Book
				{
					pstmt = conn.prepareStatement("insert into tbl_book (title, pubId) values (?,?)" );
					pstmt.setString(1, newBookTitle);
					pstmt.setInt(2, pubId);
					pstmt.executeUpdate();
					return;
				}
		}
		// Publisher not found. No publisher is linked
		System.out.println("The publisher name you entered is not in the list. Creating book without publisher");
		pstmt = conn.prepareStatement("insert into tbl_book (title) values (?)");
		pstmt.setString(1, newBookTitle);
		pstmt.executeUpdate();	
	}

	private static void deleteBook(Book book, List<Author> authors, int updateMenu) throws SQLException {
		
		PreparedStatement pstmt;
		if (updateMenu == 1) // Delete book completely
		{   // delete the records where bookId is present from all tables
			pstmt = conn.prepareStatement("delete from tbl_book_authors where bookId = ?");
			pstmt.setInt(1, book.getBookId());
			pstmt.executeUpdate();
			
			pstmt = conn.prepareStatement("delete from tbl_book_copies where bookId = ?");
			pstmt.setInt(1, book.getBookId());
			pstmt.executeUpdate();
			
			pstmt = conn.prepareStatement("delete from tbl_book_genres where bookId = ?");
			pstmt.setInt(1, book.getBookId());
			pstmt.executeUpdate();
			
			pstmt = conn.prepareStatement("delete from tbl_book_loans where bookId = ?");
			pstmt.setInt(1, book.getBookId());
			pstmt.executeUpdate();
			
			pstmt = conn.prepareStatement("delete from tbl_book where bookId = ?");
			pstmt.setInt(1, book.getBookId());
			pstmt.executeUpdate();
			
			System.out.println("Book deleted completely from tbl_book and also all other tables that has FKs to tbl_book");
		}
		else if (updateMenu == 2) // delete author from this book
		{
			// Print all the authors of this book
			Iterator<Author> iter; 
			int i;
			System.out.println("Which author you want to delete from this book: ");				
			System.out.println();
			iter = authors.iterator();
			i = 0;				
			while (iter.hasNext()) {
				i++;
				System.out.println(i + ")  " + iter.next().getAuthorName());
			}
			System.out.println(i + 1 + ")  Quit to previous");
			System.out.println();
			
			// Select an author from the list
			int authorsNumber = in.nextInt();
			
			if (authorsNumber == i + 1) return;
			else if (authorsNumber < 1 || authorsNumber > i + 1) {
				System.out.println("Invalid entry. Quitting");
				return;
			}
			
			// Get the authorId of the Author
			int authorId = 0;
			iter = authors.iterator();
			i = 0;				
			while (iter.hasNext()) {
				i++;
				if (authorsNumber == i) 
					authorId = iter.next().getAuthorID();
			}
				
			System.out.println("Deleting author from this book");
				
			pstmt = conn.prepareStatement("delete from tbl_book_authors where authorId = ? and bookId = ?");
			pstmt.setInt(1, authorId);
			pstmt.setInt(2, book.getBookId());
			pstmt.executeUpdate();
							
			System.out.println();
			System.out.println("Author successfully deleted from this book");
			System.out.println("Do you want to delete author completely? Keep in mind that this author may be author of other books");
			System.out.println("1)  Yes");
			System.out.println("2)  No");
			System.out.println();
			
			int deleteAuthorCompletely = in.nextInt();
			
			if (deleteAuthorCompletely == 1)
			{   // all records from FK tables are deleted
				pstmt = conn.prepareStatement("delete from tbl_book_authors where authorId = ?");
				pstmt.setInt(1, authorId);
				pstmt.executeUpdate();
				
				pstmt = conn.prepareStatement("delete from tbl_author where authorId = ?");
				pstmt.setInt(1, authorId);
				pstmt.executeUpdate();
				
				System.out.println();
				System.out.println("Author deleted completely");
				System.out.println();
			}
		}
		
	}

	/**
	 * A method used by Admin to update a Book title or Book's Author name
	 * @param book The book that we want to modify
	 * @param authors The list of authors of the book
	 * @param updateMenu - 1) Update book title. 2) Update book author's name
	 * @throws SQLException
	 */
	private static void updateBook(Book book, List<Author> authors, int updateMenu) throws SQLException {
		
		// Print the book details
		System.out.println();
		System.out.println("The book you are updating has the following title and authors");
		System.out.println("Title: " + book.getTitle());
		System.out.println("Authors: ");
		Iterator<Author> iter = authors.iterator();
		int i = 0;
		while (iter.hasNext()) {
			i++;
			String authorName = iter.next().getAuthorName();
			if (authorName == null) {
				System.out.println("This book has no authors");
				if (updateMenu == 2) {
					System.out.println("Nothing to update");
					return;
				}
			}
			else System.out.println(i + ")  " + authorName);
		}
		System.out.println();
		
		if (updateMenu == 1) // UPDATE book title
		{
			System.out.println("Please enter new title of the book or enter N/A for no change:");
			String newTitle = in.nextLine();
			newTitle = in.nextLine();
			if (newTitle.equals("N/A"))
				return;
			PreparedStatement pstmt = conn.prepareStatement("update tbl_book set title= ? where bookId = ?");
			pstmt.setString(1, newTitle);
			pstmt.setInt(2, book.getBookId());
			pstmt.executeUpdate();
			
			System.out.println("Book title successfully updated");
			System.out.println();
		}
		else if (updateMenu == 2) // UPDATE Author of the book
		{
			String newName = "";
				System.out.println("Please select author whose name you want to change:");
				System.out.println();
				iter = authors.iterator();
				i = 0; // counter
				
				while (iter.hasNext()) { // Print out all the Authors of this book
					i++;
					System.out.println(i + ")  " + iter.next().getAuthorName());
				}
				System.out.println(i+1 + ")  Quit to previous");
				System.out.println();
				
				// Entering a number from the list
				int authorsNumber = in.nextInt();
				
				if (authorsNumber == i + 1) return; // Quit to previous is selected
				
				else if (authorsNumber < 1 || authorsNumber > i + 1) { // Selection is not in the list. Quit.
					System.err.println("You entered incorrect menu number. Quitting");
					return;
				}
				
				int authorId = 0;
				authorId = authors.get(authorsNumber).getAuthorID();
				
				/*iter = authors.iterator();
				i = 0;				
				while (iter.hasNext()) {
					i++;
					if (authorsNumber == i) 
						authorId = iter.next().getAuthorID();
				}
				*/
				
				System.out.println("Please enter new author name of this author:");
				newName = in.nextLine();
				newName = in.nextLine();
				
				// Update tbl_author with new name
				PreparedStatement pstmt = conn.prepareStatement("update tbl_author set authorName= ? where authorId = ?");
				pstmt.setString(1, newName);
				pstmt.setInt(2, authorId);
				pstmt.executeUpdate();
				// Update Author object in the list with new name
				authors.get(authorsNumber).setAuthorName(newName);
				
				System.out.println();
				System.out.println("Author name successfully updated");
				System.out.println();
			}			
	}

	private static void librarianMenu() throws SQLException {
				
		int menu = 0;
		
		while (true) {
			// 
			System.out.println();
			System.out.println("1)  Enter Branch you manage");
			System.out.println("2)  Quit to previous");
			System.out.println();
			
			menu = in.nextInt();
			if (menu < 1 || menu > 2) {
				System.err.println(" Please enter 1 or 2:");
			}
			else if (menu == 1) {
				// List all the branches in the db
				Lib2Menu();					
				System.out.println();
			}
			else if (menu == 2)
				return;
		} 
			
	}
	
	
	/**
	 * A method that lists all the library branches and lets user to choose a library
	 * @param libraryBranches is the list of library branches
	 * @throws SQLException
	 */
	
	private static void Lib2Menu() throws SQLException {
		
		// a list containing Library Branches
		List<LibraryBranch> libraryBranches = queryLibraryBranch();
		
		int chosenLibraryBranch = printAndChooseLibraryBranch(libraryBranches); 
			
		if (chosenLibraryBranch >= 1 && chosenLibraryBranch <= libraryBranches.size())
			Lib3Menu(libraryBranches.get(chosenLibraryBranch - 1), chosenLibraryBranch); //selected libraryBranch and number
		
		return;
	}

	
	private static int printAndChooseLibraryBranch(
			List<LibraryBranch> libraryBranches) {
		
		ListIterator<LibraryBranch> iter; 
		int menu = 0;
		
		do {
			iter = libraryBranches.listIterator();
			while (iter.hasNext())
			{
				System.out.print((iter.nextIndex() + 1) + ")  ");
				LibraryBranch tmpLibBr = iter.next();
				System.out.println(tmpLibBr.getBranchName() + ", " + tmpLibBr.getBranchAddress());
			}
			System.out.println(libraryBranches.size() + 1 + ")  Quit to previous");
			System.out.println();
			
			menu = in.nextInt();
			
			if (menu < 1 || menu > libraryBranches.size() + 1) // Validating
				System.err.println("Please enter a number 1 to " + libraryBranches.size() + 1 + ":");
		
		} while (menu < 1 || menu > libraryBranches.size() + 1);
		
		return menu; //returning the number that was chosen in the library branch list
	}

	private static List<LibraryBranch> queryLibraryBranch() throws SQLException {
		
		// a list containing Library Branches
		List<LibraryBranch> libraryBranches = new ArrayList<LibraryBranch>();
		PreparedStatement pstmt = conn.prepareStatement("select branchId, branchName, branchAddress from tbl_library_branch");
		ResultSet rs = pstmt.executeQuery();
		
		while (rs.next()) {
			LibraryBranch tmpLibBr = new LibraryBranch(rs.getInt("branchID"), rs.getString("branchName"), rs.getString("branchAddress"));
			libraryBranches.add(tmpLibBr);				
		}
		return libraryBranches;
	}

	private static void Lib3Menu(LibraryBranch libraryBranch, int id) throws SQLException{
		
		int menu = 0;
		
		while (menu != 3) {
			System.out.println();
			System.out.println("1)  Update the details of the Library");
			System.out.println("2)  Add copies of Book to the Branch");
			System.out.println("3)  Quit to previous");
			System.out.println();
		
			menu = in.nextInt();
			if (menu < 1 || menu > 3) // Validation
				System.err.println("Please enter 1, 2 or 3:");
			if (menu == 1) { // Update option selected
				updateLibraryBranch(libraryBranch, id);						
			}
			else if (menu == 2) { // Add copies option selected
				System.out.println();
				System.out.println("Pick the Book you want to add copies of, to your branch:");
				addCopiesOfBookToBranch(libraryBranch);			
			}
		}		
		
		return;
	}
	

	private static void addCopiesOfBookToBranch(LibraryBranch libraryBranch) throws SQLException {
		// a list containing Books
		Map<Book, List<Author>> books = selectBooks("select b.bookId, b.title, b.pubId, a.authorId, a.authorName " 
											 			+ " from tbl_book as b " 
											 			+ " left join tbl_book_authors as ba " 
											 			+ " on b.bookId = ba.bookId " 
											 			+ " left join tbl_author as a "  
											 			+ " on ba.authorId = a.authorId;");
				
		printBooks(books); // Print out all the books followed by its authors
		
		// Choose a book, which you want to add more copies of
		int menu;
		
		do { //validate choosing
			menu = in.nextInt();
		} while (menu < 1 || menu > books.size() + 1); 
		
		if (menu <= books.size())
		{
			for (Book book : books.keySet()) {
				if (book.getBookListNumber() == menu) // if selected book = enumerated book
				{
					ResultSet rs = printExistingNoOfCopies(libraryBranch, book);
					addNewCopies(libraryBranch, book, rs);
					break;
				}
			}
		}
	}

	private static Map<Book, List<Author>> selectBooks(String query) throws SQLException {
		// books are saved in HashMap of <Book, List<Author>>
		Map<Book,List<Author>> books = new HashMap<Book, List<Author>>();
		
		// Select all books with authors. null is returned for no author.
		PreparedStatement pstmt = conn.prepareStatement(query);
		ResultSet rs = pstmt.executeQuery();
		
		while (rs.next()) {
			int bookId = rs.getInt(1);
			String bookTitle = rs.getString(2);
			int publisherId = rs.getInt(3);
			int authorId = rs.getInt(4);
			String authorName = rs.getString(5);
			
			Book book = new Book(bookId, bookTitle, publisherId);
			Author author = new Author(authorId, authorName);
			
			List<Author> authors= books.get(book);
					
			if (authors == null) 
				books.put(book, authors=new ArrayList<Author>());
			authors.add(author);
		}
		return books;
	}

	private static void printBooks(Map<Book, List<Author>> books) {
		// Print All books
		int i = 0; // counter to print the list
		Author author;

		for (Book book : books.keySet()) {
			List<Author> authors = books.get(book);
			i++;
			book.setBookListNumber(i); // setting the bookListNumber to what is seen by user
			System.out.print(i + ")  " + book.getTitle());
			ListIterator<Author> iter = authors.listIterator();
			while (iter.hasNext())
			{
				author = iter.next();
									
				// if SQL returned null for author, then there is no author for the book
				if (author.getAuthorName() == null && author.getAuthorID() == 0) 
					break;
				if (author.equals(authors.get(0))) System.out.print(" by "); // first author of the book
				if (iter.hasNext()) System.out.print(author.getAuthorName() + ", "); // not the last author of the book
				else System.out.print(author.getAuthorName()); // last author of the book
			}
			System.out.println();
		}
		System.out.println(i + 1 + ")  Quit to cancel operation");
		System.out.println();	
		
	}

	/**
	 * A method that prints all authors in the DB + suggests to create a new author.
	 * Returns number of total authors in DB. 
	 * @return The number of authors in the DB
	 * @throws SQLException
	 */
	private static List<Author> printAuthors() throws SQLException {

		System.out.println();
		System.out.println("Following is the the list of existing authors. Pick one:");
		
		int counter = 0;
		List<Author> authors = new ArrayList<Author>();
		
		PreparedStatement pstmt = conn.prepareStatement("select authorName, authorId from tbl_author" );
		ResultSet rs = pstmt.executeQuery();
		
		while (rs.next()){
			String authorName = rs.getString(1);
			int authorId = rs.getInt(2);
			authors.add(new Author(authorId, authorName));
			counter++;
			System.out.println(counter + ")  " + authorName);
		}
		System.out.println(counter + 1 + ")  Create a new author");
		System.out.println();
		
		return authors;
		
	}

	private static ResultSet printExistingNoOfCopies(LibraryBranch libraryBranch,
			Book book) throws SQLException {
		
		System.out.print("Existing number of copies: ");
		
		PreparedStatement pstmt = conn.prepareStatement("select noOfCopies from tbl_book_copies where bookId = ? and branchId = ?");
		pstmt.setInt(1, book.getBookId());
		pstmt.setInt(2, libraryBranch.getId());
		ResultSet rs = pstmt.executeQuery();
		
		int noOfCopies = 0; 
		
		if (rs.first())	
			noOfCopies = rs.getInt("noOfCopies");
		System.out.println(noOfCopies);
		System.out.println();
		
		return rs;
		
	}

	private static void addNewCopies(LibraryBranch libraryBranch, Book book, ResultSet rs) throws SQLException {
		PreparedStatement pstmt;
		
		System.out.println("Enter new number of copies:");
		int addedNoOfCopies = in.nextInt();
		
		if (!rs.first()) // there are no copies of this book in this library branch
			pstmt = conn.prepareStatement("insert into tbl_book_copies (noOfCopies, bookId, branchID) values(?,?,?)");
		else // there is a record of books in this library_branch
			pstmt = conn.prepareStatement("update tbl_book_copies set noOfCopies= ? where bookId = ? and branchId = ?");
		
		int noOfCopies = 0;
		
		if (rs.first())
			noOfCopies = rs.getInt("noOfCopies");
		
		pstmt.setInt(1, noOfCopies + addedNoOfCopies);
		pstmt.setInt(2, book.getBookId());
		pstmt.setInt(3, libraryBranch.getId());
		int rowsAffected = pstmt.executeUpdate();
		
		if (rowsAffected == 1) {
			System.out.println(addedNoOfCopies + " new book copies have been added to " + libraryBranch.getBranchName() + " library branch");
		}
		else if (rowsAffected == 0){
			System.err.println("No new books have been added.");
		}
	}
	
	private static void updateLibraryBranch(LibraryBranch libraryBranch, int id) throws SQLException {
		
		PreparedStatement pstmt;
		System.out.println("You have chosen to update the Branch with Branch Id: " + id + " and Branch Name: " + libraryBranch.getBranchName() + ".");
		System.out.println("Enter ‘quit’ at any prompt to cancel operation ");
		System.out.println();
		System.out.println("Please enter new branch name or enter N/A for no change:");
		
		String newName = in.nextLine();
		newName = in.nextLine();
		
		if (newName.toUpperCase().equals("QUIT")) return;
		else if (newName.toUpperCase().equals("N/A")) newName = libraryBranch.getBranchName(); // N/A means that user doesnt want to change Name
		System.out.println("Please enter new branch address or enter N/A for no change:");
		String newAddress = in.nextLine();
		if (newAddress.toUpperCase().equals("QUIT")) return;
		else if (newAddress.toUpperCase().equals("N/A")) newAddress = libraryBranch.getBranchAddress(); // N/A means user doesnt want to change Address
		
		pstmt = conn.prepareStatement("update tbl_library_branch set branchName=? , branchAddress=? where branchId=?");
		pstmt.setString(1, newName);
		pstmt.setString(2, newAddress);
		pstmt.setInt(3, libraryBranch.getId());
		
		libraryBranch.setBranchName(newName); // Setting name and address of Library in Java objects
		libraryBranch.setBranchAddress(newAddress);
		
		int rowsAffected = pstmt.executeUpdate();
		
		if (rowsAffected == 1) {
			System.out.println("Library branch successfully updated.");
		}
		else if (rowsAffected == 0){
			System.err.println("Library has not been updated");
		}
	}

	/**
	 * A method that greets the User and asks to identify himself/herseld as one of: librarian, borrower or admin
	 */
	private static void welcomeMessage() {
		System.out.println();
		System.out.println("Welcome to the GCIT Library Management System. Which category of a user are you");
		System.out.println();
		System.out.println("   1)  Librarian");
		System.out.println("   2)  Administrator");
		System.out.println("   3)  Borrower");
		System.out.println();
	}

}
