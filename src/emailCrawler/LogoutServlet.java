package emailCrawler;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//import org.apache.commons.lang3.StringEscapeUtils;

/** 
 * Logout Servlet, will log the user out
 *
 */
@SuppressWarnings("serial")
public class LogoutServlet extends BaseServlet {
	
	// DatabaseHandler interacts with the MySQL database
	private static final DatabaseHandler dbhandler = DatabaseHandler.getInstance();


	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		HttpSession session = request.getSession();
		session.invalidate();
		
		
		String url = "/login";
		url = response.encodeRedirectURL(url);
		response.sendRedirect(url); // send a get request  (redirect to the same path)
		//response.encodeRedirectURL("/hotels");

		//displayForm(out); 
		//finishResponse(response);
	}

	
}