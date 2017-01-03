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
 * 
 * 
 * @see EmailCrawlerServer
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

	/** Used to determine if a username already exists. */
	private static final String USER_SQL = "SELECT emailId FROM UberSpendings WHERE emailId = ?";

	/** Used to insert into UberSpendings Table */
	private static final String INSERT_UBERSPENDINGS_SQL = "INSERT into UberSpendings (emailId,date,tripPrice)"+ "VALUES(?, ?, ?)";
	
	/** This will calculate total spendings for a chosen month */
	private static final String TOTAL_SPENDINGS_FOR_MONTH = "select SUM(tripPrice) AS totalSpendingsForOctober from UberSpendings where emailId=? AND date like ? AND date like ?";
	
	/** This is to find all 2016 spendings */
	private static final String SPENDINGS_FOR_MONTH = "select * from UberSpendings where emailId=? AND date like ? AND date like ?";
	
	/** Used to configure connection to database. */
	private DatabaseConnector db;

	/**
	 * This class is a singleton, so the constructor is private. Other classes
	 * need to call getInstance()
	 */
	public DatabaseHandler() {
		Status status = Status.OK;
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
	 * database connection. If the user info is already stored, we need not crawl the inbox!
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
	
	/**
	 * This method enters all the user info - emailID, the timestamp of the trip and the price of the trip
	 * @param emailId
	 * @param date
	 * @param tripPrice
	 * @return
	 */
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
  * This method will first check if we have the user info in the database, if not, we will crawl the inbox folder
  * @param emailId
  * @param password
  * @param request
  * @param response
  * @param month
  */
 public void loginUser(String emailId, String password, HttpServletRequest request, HttpServletResponse response, String month) {
 	
     Properties props = new Properties();
     props.setProperty("mail.store.protocol", "imaps");	
     try {
     	response.getWriter();
         Session session = Session.getInstance(props, null);
         Store store = session.getStore();
         store.connect("imap.gmail.com", emailId, password);
         Folder inbox = store.getFolder("INBOX");
         inbox.open(Folder.READ_ONLY);
         int messageCount = inbox.getMessageCount();
         System.out.println(messageCount);
          BufferedWriter bw = new BufferedWriter(new FileWriter("UberEmails.txt"));
          Object content;
          DateFormat originalFormat = new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");
      	DateFormat targetFormat = new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");
          
          String price;
          Status status = duplicateUser(emailId);
          
          if(status==Status.DUPLICATE_USER){
        	  //show the user the spendings, directly retrieve it from the database
  			String url = "/showspendings?month=Dec";
  			url = response.encodeRedirectURL(url);
  			response.sendRedirect(url);
          } // Crawl the email inbox! 
          else{
         for(int i =1000;i<=messageCount;i++){
         	Message msg = inbox.getMessage(i);	
         	Address[] in = msg.getFrom();
                if(InternetAddress.toString(in).contains("Uber Receipts")) {
               
             	   bw.write("\n");
                	bw.write("---------------------------------");
                	bw.write("\n");
                	bw.write("FROM:" +InternetAddress.toString(in));
                	bw.write("\n");
                	Date date = originalFormat.parse(formatMMMyyyy(msg));
                	String formattedDate = targetFormat.format(date);
     			
     			
                	bw.write("\n");
                	bw.write("SUBJECT:" + msg.getSubject());
                	bw.write("\n");
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
                     } 
             		 
                 	} //if the email is from Uber gets over here
                 
                 
             }
     } 
         response.getWriter();	
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
 
 /**
  * This method calculates total spending for a month and also displays individual spendings during that month
  * @param emailId
  * @param month
  * @param response
  * @param request
  */
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

 /**
  * converts the date to standard date format in String
  * @param msg
  * @return
  * @throws MessagingException
  */
	private static String formatMMMyyyy(Message msg) throws MessagingException {
		return msg.getSentDate().toString();
	}
	/**
	 * This method changes the format of the date to the format d-MMM-yy for the d3 graph
	 * @param date
	 * @return
	 */
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
	
 /**
  * Made this method to display spending behaviour by using a graph, but Velocity wouldn't render it!
  * @param emailId
  * @param month
  * @param response
  * @param request
  */
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
