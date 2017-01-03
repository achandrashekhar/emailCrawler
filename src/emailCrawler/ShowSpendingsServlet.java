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
 * A servlet to calculate total spendings for a chosen Month
 *
 */
@SuppressWarnings("serial")
public class ShowSpendingsServlet extends BaseServlet {
	
	private static final DatabaseHandler dbhandler = DatabaseHandler.getInstance();


	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		prepareResponse("Logging in ", response,request);
		String month = request.getParameter("month");
		HttpSession session = request.getSession();
		String uname = (String) session.getAttribute("username");
		System.out.println("This is the showSpendingsServlet and we got the username as "+uname);
		dbhandler.getSpendingForMonth(uname,month,response,request);
		
		finishResponse(response,request);
	}	
	
}