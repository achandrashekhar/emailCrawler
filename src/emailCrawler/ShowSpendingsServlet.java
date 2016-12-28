package emailCrawler;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

//import org.apache.commons.lang3.StringEscapeUtils;

/** 
 * A servlet that handles user Login. doGet() method displays an HTML form with a button and
 * two textfields: one for the username, one for the password.
 * doPost() processes the form: if the username is not taken, it adds user info to the database.
 *
 */
@SuppressWarnings("serial")
public class ShowSpendingsServlet extends BaseServlet {
	
	ReadingEmail readEmailObject = new ReadingEmail();


	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		prepareResponse("Logging in ", response,request);
		String month = request.getParameter("month");
		readEmailObject.getSpendingForMonth(month,response,request);
		
		finishResponse(response,request);
	}	
	
}