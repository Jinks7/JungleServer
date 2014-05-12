package chat.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileIO {
    
    private File file;
    private BufferedReader reader;
    private PrintWriter writer;
    private Logger log;
    
    private String s;
    
    public FileIO(String directory, String fi, boolean app){
        log = new Logger();
        s = System.getProperty("file.separator");
        
        File dir = new File(directory);
        if (!dir.exists()){
            dir.mkdirs();
        }
        
        // get the path to the file
        file = new File(directory + s, fi);
        
        try {
            if (!file.canWrite()){
                file.createNewFile();
            }
            
            reader = new BufferedReader(new FileReader(file));
            writer = new PrintWriter(new FileWriter(file, app));
        } catch (FileNotFoundException e){
            log.writeErr("Could not access file: " + file.getAbsolutePath() + "\nError was: " + e.getMessage());
        } catch (IOException ex) {
            log.writeErr("Could not read/write to file: " + file);
        }
        
        
    }
    
    public void deleteFile(){
        // remove the file that is being used by this class
        close(); // remove the streams
        
        if (file.delete()){
            log.writeLn("File " + file.getAbsolutePath() + " was deleted.");
        } else {
            log.writeLn("File could not be deleted. \nFile: " + file.getAbsolutePath());
        }
    }
    
    public String readLn() throws IOException{
        try {
            return reader.readLine();
        } catch (IOException e){
            return "";
        }
    }
    
    public void writeLn(String line){
        try {
            writer.println(line);
            writer.flush();
        } catch (Exception e){
            log.writeErr("Could not write to file.\nFile: " + file.getAbsolutePath() + "\nError was: " + e);
            //close();
        }
    }
    
    public void write(String line) {
        try {
            writer.print(line);
            writer.flush();
        } catch (Exception e){
            log.writeErr("Could not write to file.\nFile: " + file.getAbsolutePath() + "\nError was: " + e.getMessage());
            close();
        }
    }    
    
    
    public void close(){
        try {
            reader.close();
            writer.close();
            
            //reader = null;
            //writer = null;
        } catch (IOException e){
        }
    }
    
}
