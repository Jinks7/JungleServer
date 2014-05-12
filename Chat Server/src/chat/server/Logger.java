package chat.server;

// print command line stuff

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Logger {
    
    static BufferedReader reader;
    
    public Logger(){
        reader = new BufferedReader(new InputStreamReader(System.in));
    }
    
    
    public void write(String s){
        System.out.print(s);
    }
    
    public void write(Object s){
        System.out.print(s);
    }
    
    public void writeLn(String s){
        System.out.println(s);
    }
    
    public void writeLn(boolean s){
        System.out.println(s);
    }
    
    public void writeErr(String s){
        System.err.println("Error: " + s);
    }
    
    public void writeErr(Object s){
        System.err.println("Error: " + s);
    }
    
    public String readLn(){
        try {
            return reader.readLine();
        } catch (IOException ex) {
            write("The input could not be read!");
            return null;
        }
    }
    
    public String readPassword(){
        EraserThread et = new EraserThread();
        Thread th = new Thread(et);
        th.start();
        
        String password = "";
        
        try {
            password = readLn();
        } catch (Exception e){
            writeErr(e);
        }
        
        et.stopMasking();
        
        return password;
    }
    
    // havent found a working method as of yet
    public void clearConsole() {
        for (int i=0; i<80; i++)
        System.out.println("");
        
        System.out.print("\r\r\r");        
    }
    
    public void flush(){
        System.out.flush();
    }
    
    public void getProperties() {
        System.getProperties().list(System.out);
    }
    
}
