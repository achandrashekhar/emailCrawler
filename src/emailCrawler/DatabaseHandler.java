package emailCrawler;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;



/**
 * Handles all database-related actions. Uses singleton design pattern. Modified
 * by Prof. Karpenko from the original example of Prof. Engle.
 * 
 * @see RegisterServer
 */
public class DatabaseHandler {

	/** Makes sure only one database handler is instantiated. */
	private static DatabaseHandler singleton = new DatabaseHandler();

	/** Used to determine if login_users table exists. */
	private static final String TABLES_SQL = "SHOW TABLES LIKE 'UberSpendings';";

	/** Used to create login_users table for this example. */
	private static final String CREATE_SQL = "CREATE TABLE login_users ("
			+ "userid INTEGER AUTO_INCREMENT PRIMARY KEY, " + "username VARCHAR(32) NOT NULL UNIQUE, "
			+ "password CHAR(64) NOT NULL, " + "usersalt CHAR(32) NOT NULL);";

	/** Used to insert a new user's info into the login_users table */
	private static final String REGISTER_SQL = "INSERT INTO login_users (username, password, usersalt) "
			+ "VALUES (?, ?, ?);";

	/** Used to determine if a username already exists. */
	private static final String USER_SQL = "SELECT emailId FROM UberSpendings WHERE emailId = ?";

	// ------------------ constants below will be useful for the login operation
	// once you implement it
	/** Used to retrieve the salt associated with a specific user. */
	private static final String SALT_SQL = "SELECT usersalt FROM login_users WHERE username = ?";

	/** Used to authenticate a user. */
	private static final String AUTH_SQL = "SELECT username FROM login_users " + "WHERE username = ? AND password = ?";

	/** Used to remove a user from the database. */
	private static final String DELETE_SQL = "DELETE FROM login_users WHERE username = ?";
	
	/** Used to insert into UberSpendings Table */
	private static final String INSERT_UBERSPENDINGS_SQL = "INSERT into UberSpendings (emailId,date,tripPrice)"+ "VALUES(?, ?, ?)";
	/**Used to check if user already has all data updated */
	private static final String CHECK_FOR_EXISTING_USER = "SELECT * from UberSpendings where emailId = ?";
	/**
	 * This is stuff from the previous Project, I will re cycle from here
	 */
	private static final String TOTAL_SPENDINGS_FOR_MONTH = "select SUM(tripPrice) AS totalSpendingsForOctober from UberSpendings where emailId=? AND date like ? AND date like ?";
	/** This is to find all 2016 spendings */
	private static final String SPENDINGS_FOR_MONTH = "select * from UberSpendings where emailId=? AND date like ? AND date like ?";
	
	private static final String REVIEW_SQL = "SELECT * from reviews where hotelId = ?";
	private static final String ADD_REVIEW = "INSERT into reviews (reviewId,hotelID,reviewTitle,reviewText,userName,date,overallRating)"+ "VALUES(?, ?, ?, ?, ?, ?, ?)";
	private static final String AVG_RATING = "SELECT AVG(overallRating) as average from reviews where hotelId=?";
	private static final String KEYWORD_SEARCH = "SELECT * from hotelDataTable where hotelName LIKE ? ESCAPE '!'";
	private static final String PARTICULAR_HOTEL = "SELECT * FROM hotelDataTable where hotelId=?";
	private static final  String SEARCH_BY_PLACE = "SELECT * FROM hotelDataTable WHERE city = ? OR state = ? OR country = ? ";
	
	private static final String UPDATE_REVIEW = "UPDATE reviews SET reviewTitle = ?,reviewText = ?,overAllRating = ?,date=? WHERE userName=? AND hotelID=? ";
	private static final String SAVE_HOTEL = "INSERT INTO savedHotels(userName,hotelID,hotelName)"+"VALUES(?,?,?)";
	private static final String GET_SAVED_HOTELS = "SELECT * from savedHotels where userName = ?";
	private static final String GET_LAST_LOGIN = "SELECT * from lastLogin where userName=?";
	private static final String INSERT_LAST_LOGIN = "INSERT into lastLogin(userName,lastLoginDate)"+"VALUES(?,?)";
	private static final String UPDATE_LAST_LOGIN ="UPDATE lastLogin SET lastLoginDate=? where userName=?";
	private static final String SORT_REVIEWS_BY_DATE = "SELECT * FROM reviews where hotelID=? ORDER BY date DESC";
	private static final String SORT_REVIEWS_BY_RATING = "SELECT * FROM reviews where hotelID=? ORDER BY overAllRating DESC";
	private static final String CLEAR_SAVED_HOTELS = "DELETE FROM savedHotels where userName=? ";
	private static final String SAVE_EXPEDIA_LINK = "INSERT INTO expediaLinks(userName,hotelID,link)"+"VALUES(?,?,?)";
	private static final String GET_SAVED_LINKS = "SELECT * from expediaLinks where userName = ?";
	private static final String CLEAR_EXPEDIA_LINKS = "DELETE FROM expediaLinks where userName=? ";
	private static final String PAGINATED_REVIEWS = "SELECT * FROM reviews where hotelID=? LIMIT ?,5";
	private static final String REVIEWS_COUNT = "SELECT COUNT(reviewID) from reviews where hotelID=?";
	/** Used to configure connection to database. */
	private DatabaseConnector db;

	/** Used to generate password hash salt for user. */
	private Random random;

	/**
	 * This class is a singleton, so the constructor is private. Other classes
	 * need to call getInstance()
	 */
	public DatabaseHandler() {
		Status status = Status.OK;
		random = new Random(System.currentTimeMillis());

		try {
			db = new DatabaseConnector("database.properties");
			status = db.testConnection() ? setupTables() : Status.CONNECTION_FAILED;
		} catch (FileNotFoundException e) {
			status = Status.MISSING_CONFIG;
		} catch (IOException e) {
			status = Status.MISSING_VALUES;
		}

		if (status != Status.OK) {
			System.out.println("Error while obtaining a connection to the database: " + status);
		}
	}

	/**
	 * Gets the single instance of the database handler.
	 *
	 * @return instance of the database handler
	 */
	public static DatabaseHandler getInstance() {
		return singleton;
	}

	/**
	 * Checks to see if a String is null or empty.
	 * 
	 * @param text
	 *            - String to check
	 * @return true if non-null and non-empty
	 */
	public static boolean isBlank(String text) {
		return (text == null) || text.trim().isEmpty();
	}

	/**
	 * Checks if necessary table exists in database, and if not tries to create
	 * it.
	 *
	 * @return {@link Status.OK} if table exists or create is successful
	 */
	private Status setupTables() {
		Status status = Status.ERROR;

		try (Connection connection = db.getConnection(); Statement statement = connection.createStatement();) {
			if (!statement.executeQuery(TABLES_SQL).next()) {
				// Table missing, must create
				statement.executeUpdate(CREATE_SQL);

				// Check if create was successful
				if (!statement.executeQuery(TABLES_SQL).next()) {
					status = Status.CREATE_FAILED;
				} else {
					status = Status.OK;
				}
			} else {
				status = Status.OK;
			}
		} catch (Exception ex) {
			status = Status.CREATE_FAILED;
		}

		return status;
	}

	/**
	 * Tests if a user already exists in the database. Requires an active
	 * database connection.
	 *
	 * @param connection
	 *            - active database connection
	 * @param user
	 *            - username to check
	 * @return Status.OK if user does not exist in database
	 * @throws SQLException
	 */
	private Status duplicateUser(String user) {

		
		assert user != null;

		Status status = Status.ERROR;
		try (Connection connection = db.getConnection();) {
		try (PreparedStatement statement = connection.prepareStatement(USER_SQL);) {
			statement.setString(1, user);

			ResultSet results = statement.executeQuery();
			status = results.next() ? Status.DUPLICATE_USER : Status.OK;
		} 
		}catch (SQLException e) {
			status = Status.SQL_EXCEPTION;
			System.out.println("Exception occured while processing SQL statement:" + e);
		}

		return status;
	}

	
	

	

	
	
	
	
	public Status createUberSpendingsTable(String emailId, String date, double tripPrice){
		Status status=Status.OK;
		
		try (Connection connection = db.getConnection();) {
			
			try (PreparedStatement statement = connection.prepareStatement(INSERT_UBERSPENDINGS_SQL);) {
				statement.setString(1, emailId);
				statement.setString(2, date);
				statement.setDouble(3, tripPrice);
				statement.executeUpdate();
				status = Status.OK;
			}
			
		} catch (SQLException ex) {
			status = Status.CONNECTION_FAILED;
			System.out.println("Error while connecting to the database: " + ex);
		} 
		return status;
	}
	
	 
	 
	
	
	
 /**
  * Look up if user has already entered a review
  * @param emailId
  * @param hotelID
  * @return
  */
 public Status lookUpExistingUserReview(String emailId){
		Status status=Status.OK;
		try (Connection connection = db.getConnection();) {
			try (PreparedStatement statement = connection.prepareStatement(CHECK_FOR_EXISTING_USER);) {
				statement.setString(1, emailId);
			
				ResultSet results = statement.executeQuery();
				status = results.next() ? Status.OK: Status.ERROR;
				
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return status;
	}
 
 
 public void loginUser(String emailId, String password, HttpServletRequest request, HttpServletResponse response, String month) {
 	//System.out.println("credentials supplied are "+ emailId+", "+password);
     Properties props = new Properties();
     props.setProperty("mail.store.protocol", "imaps");
//     HttpSession usernameSession = request.getSession();
//		usernameSession.setAttribute("username", emailId);
		
		
     try {
     	PrintWriter writer = response.getWriter();
         Session session = Session.getInstance(props, null);
         Store store = session.getStore();
         store.connect("imap.gmail.com", emailId, password);
         Folder inbox = store.getFolder("INBOX");
         inbox.open(Folder.READ_ONLY);
         int messageCount = inbox.getMessageCount();
         System.out.println(messageCount);
          BufferedWriter bw = new BufferedWriter(new FileWriter("UberEmails.txt"));
          Object content;
          // code for converting new date, this needs to be a seperate function
          DateFormat originalFormat = new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");
      	DateFormat targetFormat = new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");
          
          String price;
          Status status = duplicateUser(emailId);
          
          if(status==Status.DUPLICATE_USER){
        	  //show the user the spendings directly
        	  
  			String url = "/showspendings?month=Dec";
  			url = response.encodeRedirectURL(url);
  			response.sendRedirect(url);
          }
          
          else{
         for(int i =1000;i<=messageCount;i++){
//         	bw.write("\n");
//         	bw.write("Reading email number: "+i);
         	Message msg = inbox.getMessage(i);	
         	Address[] in = msg.getFrom();
         	

         	
             
                if(InternetAddress.toString(in).contains("Uber Receipts")) {
               
             	   bw.write("\n");
                	bw.write("---------------------------------");
                	bw.write("\n");
                	bw.write("FROM:" +InternetAddress.toString(in));
             	
             	bw.write("\n");
             	
             	// for date checking
             	
             	Date date = originalFormat.parse(formatMMMyyyy(msg));
             	String formattedDate = targetFormat.format(date);
     			
     			
     			bw.write("\n");
     			bw.write("SUBJECT:" + msg.getSubject());
     			bw.write("\n");
     			//Iterate over all the months
     			//for(String key : allSpendings.keySet()){
                     // check for the month's Spendings
     			//if(formattedDate.equals(key+ " 2016")){
     			bw.write("SENT DATE:" + formattedDate);
                     content = msg.getContent();  
                     if (content instanceof String)  
                     {  
                     	System.out.println("It's going here");
                         String body = (String)content;  
                         bw.write("\n");
                         bw.write("CONTENT:"+body);
                         
                     }  
                     else if (content instanceof Multipart)  
                     {  
                         Multipart mp = (Multipart)content;  
                         BodyPart bp = mp.getBodyPart(0);
                         bw.write("\n");
                        
                         
                          String multiPartText = (String) bp.getContent();
                          
                          
                          Document doc = Jsoup.parse(multiPartText);
                          Elements element = doc.getElementsByClass("totalPrice topPrice tal black");
                          Elements elementHeaderPrice = doc.getElementsByClass("header-price");
                          if(element.text().equals("")){
                        	  System.out.println("Got this price initially "+elementHeaderPrice.text());
                        	  if(elementHeaderPrice.text().equals("")){
                        		  System.out.println("Didn't get any text here");
                        		  price = "0";
                        	  }
                        	  else {
                         	price = elementHeaderPrice.text().substring(1);
                         	System.out.println("got this price now "+price);
                        	  }
                          }
                          else {
                         	 price = element.text().substring(1);
                          }
                          if(price.equals("")){
                          bw.write("Fu*king Uber changed their front end AGAIN");
                          } else {
                        	  createUberSpendingsTable(emailId, formattedDate, Double.parseDouble(price));
                         	 bw.write("\nCost for the month of "+formattedDate+" is "+price);
                          }
                         
                          //totalSpending = allSpendings.get(formattedDate);
                         
                        //  totalSpending += Double.parseDouble(element.text().substring(1));
                          //allSpendings.put(formattedDate, totalSpending);
                         
                         
                     }  
 
                      	
             //   }// date if	
             			
             			
            //    } // for iterating over the map
             		 
                 	} //if the email is from Uber gets over here
                 
                 
             }
     } // we already have all user info 
     

         writer = response.getWriter();	
         bw.close();
         String url = "/showspendings?month=Dec";
			url = response.encodeRedirectURL(url);
			response.sendRedirect(url);
         System.out.println("finished reading "+messageCount+" Emails!");
         System.out.println("Done");
         	
         
         
         
     } catch (Exception mex) {
     	String url = "/login?error=" + "Wrong_Credentials_Try_Again";
			url = response.encodeRedirectURL(url);
			try {
				response.sendRedirect(url);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
         mex.printStackTrace();
     }
 }
 
 public void getSpendingForMonth(String emailId,String month, HttpServletResponse response, HttpServletRequest request) {
 	PrintWriter writer;
 	Map <String,Double> monthSpending = new HashMap<String,Double>();
 	System.out.println("You selected the month "+month);
 	String date;
 	 double spendingForTheMonth = 0;
 	 double spending;
		try {
			try (Connection connection = db.getConnection();) {
				
				try (PreparedStatement statement = connection.prepareStatement(TOTAL_SPENDINGS_FOR_MONTH);) {
					statement.setString(1, emailId);
					statement.setString(2,"%" + month + "%");
					statement.setString(3, "%"+"2016"+"%");
					ResultSet results = statement.executeQuery();
					if(results.next()) {
					spendingForTheMonth = results.getDouble(1);
					} 
					else {
						spendingForTheMonth = 0;
					}
				}
				
				try (PreparedStatement statement2 = connection.prepareStatement(SPENDINGS_FOR_MONTH);) {
					statement2.setString(1, emailId);
					statement2.setString(2, "%"+month+"%");
					statement2.setString(3, "%"+"2016"+"%");
					ResultSet results2 = statement2.executeQuery();
					while(results2.next()){
					date = results2.getString(2);
					spending = results2.getDouble(3);
					monthSpending.put(date, spending);
					}
				}
				
				
				
		 	} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			writer = response.getWriter();
			VelocityEngine ve = (VelocityEngine)request.getServletContext().getAttribute("templateEngine");
			VelocityContext context = new VelocityContext();
			Template template = ve.getTemplate("HTML_PAGES/showSpendings.html");
			
			context.put("month", month);
			context.put("monthSpending", monthSpending);
			context.put("spendingForTheMonth", spendingForTheMonth);
			context.put("key", month);
			template.merge(context, writer);	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 	
 	
 }

 
	private static String formatMMMyyyy(Message msg) throws MessagingException {
		return msg.getSentDate().toString();
	}
	
	protected String getDate(String date) {
		 
		 try {
			 DateFormat originalFormat = new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");
			 DateFormat newFormat = new SimpleDateFormat("d-MMM-yy");
			Date fdate = originalFormat.parse(date);
			String stringDate = newFormat.format(fdate);
			return stringDate;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
		
		
		
	}
 
	public void getGraphSpendingForMonth(String emailId,String month, HttpServletResponse response, HttpServletRequest request) {
	 	PrintWriter writer;
	 	System.out.println("You selected the month "+month);
	 	String date;
	 	 
	 	 double spending;
			try {
				
				BufferedWriter bw = new BufferedWriter(new FileWriter("HTML_PAGES/data2.csv"));
				bw.write("date,close\n");
				try (Connection connection = db.getConnection();) {
					
	
					
					try (PreparedStatement statement2 = connection.prepareStatement(SPENDINGS_FOR_MONTH);) {
						statement2.setString(1, emailId);
						statement2.setString(2, "%"+month+"%");
						statement2.setString(3, "%"+"2016"+"%");
						ResultSet results2 = statement2.executeQuery();
						while(results2.next()){
						date = results2.getString(2);
						String tempdate = getDate(date);
						spending = results2.getDouble(3);
						bw.write(tempdate);
						bw.write(",");
						String tempSpending = Double.toString(spending);
						bw.write(tempSpending);
						bw.write("\n");
						
						}
						bw.close();
					}
					
					
					
					
			 	} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				writer = response.getWriter();
				VelocityEngine ve = (VelocityEngine)request.getServletContext().getAttribute("templateEngine");
				VelocityContext context = new VelocityContext();
				Template template = ve.getTemplate("HTML_PAGES/index.html");
				
				context.put("month", month);
				template.merge(context, writer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
	 	
	 }
}
