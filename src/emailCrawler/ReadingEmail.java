package emailCrawler;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class ReadingEmail {
    public void loginUser(String emailId, String password, HttpServletRequest request, HttpServletResponse response) {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        HttpSession usernameSession = request.getSession();
		usernameSession.setAttribute("username", emailId);
        try {
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
         	DateFormat targetFormat = new SimpleDateFormat("MMM yyyy");
             double totalSpending = 0;
            for(int i =3000;i<=messageCount;i++){
//            	bw.write("\n");
//            	bw.write("Reading email number: "+i);
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
                        // check for October Spendings
        			if(formattedDate.equals("Dec 2016")){
        			bw.write("SENT DATE:" + formattedDate);
                        content = msg.getContent();  
                        if (content instanceof String)  
                        {  
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
                             totalSpending += Double.parseDouble(element.text().substring(1));
                            
                        }  
    
                         	
                   }// date if	
                			
                			

                		 
                    	} //if the email is from Uber gets over here
                    
                    
                }
            bw.write("Total Spendings for December are: "+ totalSpending);
            bw.close();
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

	private static String formatMMMyyyy(Message msg) throws MessagingException {
		return msg.getSentDate().toString();
	}
}