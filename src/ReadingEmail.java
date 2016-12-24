import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.InternetAddress;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class ReadingEmail {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect("imap.gmail.com", "ashi5393@gmail.com", "subbalakshmi");
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            int messageCount = inbox.getMessageCount();
            System.out.println(messageCount);
             BufferedWriter bw = new BufferedWriter(new FileWriter("UberEmails.txt"));
             Object content;
             // code for converting new date, this needs to be a seperate function
             DateFormat originalFormat = new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");
         	DateFormat targetFormat = new SimpleDateFormat("MMM yyyy");
             
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
        			bw.write("SENT DATE:" + formattedDate);
        			
        			bw.write("\n");
        			bw.write("SUBJECT:" + msg.getSubject());
        			bw.write("\n");
                        
        			//if(){
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
                             bw.write("Cost for this trip: " + element.text());
                        }  
    
                         	
                 //  }// date if	
                			
                			

                		 
                    	} //if the email is from Uber gets over here
                    
                    
                }
            bw.close();
            System.out.println("finished reading "+messageCount+" Emails!");
            System.out.println("Done");
            	
            
            
            
        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }

	private static String formatMMMyyyy(Message msg) throws MessagingException {
		return msg.getSentDate().toString();
	}
}