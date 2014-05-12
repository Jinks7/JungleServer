
package chat.server;

// this is what directly communicates with log files and conf files

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Channel {
    
    private final String channelName;
    private String topic;
    
    private FileIO conf;
    private FileIO log;
    
    public List<Client> channelClientList;
    public List<String> admin; 
    public List<String> banned;
    
    ServerThread handler;
    
    public Channel(String name, ServerThread t){
        channelName = name;
        topic = "";
        channelClientList = new ArrayList<Client>(); // contain a list of each user class
        banned = new ArrayList<String>();
        admin = new ArrayList<String>(); 
        
        conf = new FileIO("IRChat" + ServerThread.seperator + "conf", name + ".conf", true);
        log = new FileIO("IRChat" + ServerThread.seperator + "logs", name + ".log", true);
        
        log("Channel " + name + " started at " + new Date());
        
        handler = t;
        
        // set topic
        topic = findTopic();
        log("Topic is: " + topic);
        
        findAdmins();
        
    }
    
    private String findTopic(){
        try {
            String temp = "";
            conf.close();
            conf = new FileIO("IRChat" + ServerThread.seperator + "conf", channelName + ".conf", true);
            while ((temp = conf.readLn()) != null){
                if (temp.startsWith("TOPIC: ")){
                    temp = temp.substring(7);
                    return temp;
                }
            }
            
            return "";
            
        } catch (IOException ex) {
            log("ERROR: could not read topic.\n" + ex.getMessage());
            return "";
        }
        
    }
    
    private void findAdmins(){
        try {
            String temp = "";
            while ((temp = conf.readLn()) != null){
                if (temp.startsWith("ADMINS: ")){
                    temp = temp.substring(8);
                    
                    String[] t = temp.split(",");
                    for (String t1 : t) {
                        if (!"".equals(t1)) {
                            admin.add(t1);
                        }
                    }
                    
                }
            }
        } catch (IOException ex) {
            log("ERROR: could not read channel admins.\n" + ex.getMessage());
        }
    }
    
    public void log(String msg){
        try {
            log.writeLn(msg);
        } catch (Exception e){}
    }
    
    public void send(String msg){
        log(msg );
        
        //log(ServerThread.clientList.size());
        for (int i=0;i<channelClientList.size();i++){
            channelClientList.get(i).send("msg " + channelName + " " + msg);
        }
    }
    
    public void sendPure(String msg){
        log(msg );
        
        //log(ServerThread.clientList.size());
        for (int i=0;i<channelClientList.size();i++){
            channelClientList.get(i).send(msg);
        }
    }
    
    public void sendNotify(String msg){
        log("IMPORTANT: " + msg);
    }
    
    public void sendToUser(String msg, String user){
        
    }
    
    public void setAdmin(Client name){
        log("Setting " + name.getName() + " as admin.");
        admin.add(name.getName());
        
        handler.returnClient(name.getName()).send("channeladmin " + channelName);
        
        FileIO file = new FileIO("IRChat" + ServerThread.seperator + "conf", channelName + ".conf", false);
        file.writeLn("NAME: " + channelName);
        file.writeLn("TOPIC: " + topic);

        String admins = "";
        for (int i=0;i<admin.size();i++){
            admins += admin.get(i) + ",";
        }        
        file.writeLn("ADMINS: " + admins);
        file.close();
    }
    
    public void rmAdmin(String name){
        log("Removing " + name + " as admin.");
        admin.remove(name);
        
        handler.returnClient(name).send("rmchanneladmin " + channelName);
        
        FileIO file = new FileIO("IRChat" + ServerThread.seperator + "conf", channelName + ".conf", false);
        file.writeLn("NAME: " + channelName);
        file.writeLn("TOPIC: " + topic);

        String admins = "";
        for (int i=0;i<admin.size();i++){
            admins += admin.get(i) + ",";
        }        
        file.writeLn("ADMINS: " + admins);
        file.close();
    }
    
    public boolean isAdmin(String loggedinname){ // needs to be logged in name instead of name
        for (int i=0;i<admin.size();i++){
            if (admin.get(i) == null ? loggedinname == null : admin.get(i).equals(loggedinname)){
                return true;
            }
        }
        return false;
    }
    
    public boolean getAdmin(){
        return true;
    }
    
    public String sendClients(){
        String temp = "";
        for (int i=0;i<channelClientList.size();i++){
            if (!"".equals(channelClientList.get(i).getNickname())){
                if (channelClientList.get(i).getLevel() == 0){
                    temp += channelClientList.get(i).getNickname() + "++,";
                } else if (admin.indexOf(channelClientList.get(i).getName()) != -1){
                    temp += channelClientList.get(i).getNickname() + "+,";
                } else{
                    temp += channelClientList.get(i).getNickname() + ",";
                }                
            }
        }
        return temp;
    }
    
    public void addUser(Client name){ // search through admin to find if it is an admin if it is notify the client
        channelClientList.add(name);
        
        if (isAdmin(name.getName())){
            name.send("channeladmin " + channelName);
        }
        
    }
    
    public void rmUser(String name){
        for (int i=0;i<channelClientList.size();i++){
            if (channelClientList.get(i).getNickname() == name){
                channelClientList.remove(i);
            }
        }
    }
    
    public Client returnClient(String logged){
        for (int i=0; i<ServerThread.clientList.size();i++){
            if (ServerThread.clientList.get(i).getName() == null ? logged == null : ServerThread.clientList.get(i).getName().equals(logged)){
                return ServerThread.clientList.get(i);
            }
        }
        return null;
    }
    
    public void setTopic(String newTopic){
        // create lock file and echo the old file into that until it finds topic
        // then change that line and echo it back to the old file
        // finally delete lock file
        FileIO file = new FileIO("IRChat" + ServerThread.seperator + "conf", channelName + ".conf", false);
        file.writeLn("NAME: " + channelName);
        file.writeLn("TOPIC: " + newTopic);
        
        String admins = "";
        for (int i=0;i<admin.size();i++){
            admins += admin.get(i) + ",";
        }        
        file.writeLn("ADMINS: " + admins);
        file.close();
        
        topic = findTopic();
    }
    
    public void addBan(String name){
        if (!"".equals(name.trim())){
            banned.add(name.trim());
        }
    }
    
    public void rmBan(String name){
        if (!"".equals(name.trim())){
            banned.remove(name.trim());
        }
    }
    
    public void rmChannel(){
        
        for (int i=0;i<channelClientList.size();i++){
            channelClientList.get(i).send("kicked " + channelName);
        }
        
        admin = null;
        banned = null;
        channelClientList = null;
        
        conf.deleteFile();
        log.deleteFile();
        
        close();
    }
    
    public String getChannelName(){
        return channelName;
    }
    
    public String getTopic(){
        return topic;
    }
    
    public void close(){
        
        // disconnect everyone from the channel
        log("Disconnecting clients");
        
        log("Closing channel");
        
        conf.close();
        log.close();
        
        conf = null;
        log = null;
    }
    
}
