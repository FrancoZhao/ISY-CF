/*
 * The MIT License
 *
 * Copyright 2019 gfoster.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Properties;

import security.SecurityClient;

/**
 *
 * @author gfoster
 */
public abstract class DataCollector extends DatabaseConnector{
    
    private SecurityClient sc = null;

    private int schooYear; // the start of the school year

        // This runs when the instance is created.
    protected DataCollector() {
        String error = connect();
        if (error.isEmpty()){
            System.out.println("Connection to the database has been established.");
        }else {
            System.out.println(error);
            ErrorMessage.display("An error occurred while trying to connect to the database.", error);
        }
        schooYear = Calendar.getInstance().get(Calendar.YEAR);
        // If it is between January and June subtract a year 
        if (Calendar.getInstance().get(Calendar.MONTH) < 7){
            schooYear --;
        }
    } // end of constructor DataCollector
    
    protected String getStartDate(){
        return "\"" + schooYear + "-07-01%\"";
    } // end of method getStartDate()

    protected String getEndDate(){
        return "\"" + (schooYear+1) + "-06-30%\"";
    } // end of method getStartDate()

    protected String getBetweenSchoolYear(){
        return "BETWEEN " + getStartDate() + " AND " + getEndDate();
    } // end of method getSchoolYear()
    
    private void createSecurityConnection(){
        try(FileInputStream f = new FileInputStream("db.properties")) {
            // load the properties file
            Properties prop = new Properties();
            prop.load(f);

            // assign db parameters
            int port = Integer.parseInt(prop.getProperty("port"));
            String IPAddr = prop.getProperty("IP");
            sc = new SecurityClient(IPAddr, port);
            sc.start();
        } catch(IOException e) {
           System.out.println(e.getMessage());
        }
    } // end of method createSecurityConnection()

    
    @Override
    public void finalize() throws Throwable{
        close();
        super.finalize();
    }
    
    protected ResultSet doQuery(String sql){
        ResultSet rec;
        try {
            rec = st.executeQuery(sql);
        } catch (SQLException e) {
            String error = "Connection lost, retrying";
            ErrorMessage.display("An error occurred while reading data from the database.", error);
            System.out.println(error);
            return emergencyQuery(sql);
        }
        return rec;
    } // end of method doQuery
    
    private ResultSet emergencyQuery(String sql){
        String error = connect();
            if (error.isEmpty()){
                System.out.println("Connection to the database has been established.");
            }else {
            System.out.println(error);
            ErrorMessage.display("An error occurred while trying to connect to the database.", error);
        }

        ResultSet rec;
        try {
            rec = st.executeQuery(sql);
        } catch (SQLException s) {
            error = "SQL error: "
                    + s.toString() + "\n\n"
                    + s.getErrorCode() + "\n\n"
                    + s.getSQLState();
            ErrorMessage.display("An error occurred while reading data from the database.", error);
            System.out.println(error);
            return null;
        }
        String msg = "Connection re-established";
            ErrorMessage.display("Information Message", "Connection to the database.", msg);
            System.out.println(msg);
        return rec;
    } // end of method emergencyQuery()
    
    protected String insertDatabase(String sql){
        createSecurityConnection(); // establish a secure write connection to the database
        do { } while (!sc.isAuthenticated() && !sc.isRejected());
        
        if (sc.isRejected()){
            String text = "A secure connection with the database could not be established.";
            String error = "You may need to request a new password.";
            ErrorMessage.display(text, error);
            return null;
        }
        String message = sc.insertDatabase(sql);
        sc.exit();
        return message;
    } // end of method updateDatabase()
    
} // end of class DataCollector
