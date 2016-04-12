/**
 * 
 */

/**
 * @author Meirbek
 *	A class that represents a Book with a title, authors and publisher
 */
public class Book {
	private String title;	
	private int publisherId;
	private int bookId;
	private int bookListNumber; // the number of the book in printed list
	
	/**
	 * @param title Title of the Book
	 * @param publisher Publisher Name of the Book
	 * $param bookId Unique book ID representing the book
	 */
	public Book(int id, String title, int publisherId) {
		this.bookId = id;
		this.title = title;
		this.publisherId = publisherId;
	}
	
	public int getBookListNumber() {
		return bookListNumber;
	}

	public void setBookListNumber(int bookListNumber) {
		this.bookListNumber = bookListNumber;
	}

	public int getBookId() {
		return bookId;
	}

	public void setBookId(int bookId) {
		this.bookId = bookId;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public int getPublisher() {
		return publisherId;
	}
	public void setPublisher(int publisherId) {
		this.publisherId = publisherId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bookId;
		result = prime * result + publisherId;
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Book other = (Book) obj;
		if (bookId != other.bookId)
			return false;
		if (publisherId != other.publisherId)
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}
	
}
