package chat.server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ServerThread implements Runnable {
    
    private boolean run;
    private boolean ready;
    
    private ServerSocket socket;
    private Socket clientSocket;
    private final Logger log;
    private Thread remove;
    
    private String name;
    
    public static String seperator;
    
    public static final int MAXCLIENTS = 500;
    
    // hold all clients
    public static List<Client> clientList;
    // list of all channels
    public static List<Channel> channelList;
    
    public static List<String> bannedList;
    
    ChatServer server;
    
    public ServerThread(ChatServer chat){
        clientList = new ArrayList<>();
        channelList = new ArrayList<>();
        bannedList = new ArrayList<>();
        
        log = new Logger();
        seperator = System.getProperty("file.separator");
        
        log.writeLn("Creating directories for config files and logs");
        
        FileIO f = new FileIO("IRChat" + seperator + "conf", ".d", true); // make sure this directory is already created
        f.deleteFile();
        f = new FileIO("IRChat" + seperator + "logs", ".d", true); // make sure this directory is already created
        f.deleteFile();
        
        // look through directory to find list of channel config files
        getChannels();
        
        
        server = chat;
        
    }
    
    @Override
    public void run() {
        name = Thread.currentThread().getName();
        write("Opening up socket.");
        try {
            socket = new ServerSocket(2226);
        } catch (IOException e){
            writeErr("Could not open port 2226. \nEither an instance of Chat Server is already \nrunning or another program is using the \nsocket.");
            System.exit(-1);
        }
        run = true;
        
        while (run){
            try {
                // wait for clients if the maxclients isnt maxed out
                clientSocket = socket.accept();
                if (getClients() < MAXCLIENTS){
                    clientList.add(new Client(clientSocket, clientList.size(), this)); // need a better way to get client index                    
                } else {
                    log.writeLn("Too many clients. MAXCLIENTS IS: " + MAXCLIENTS);
                    try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                        out.write("d");
                    }
                }
            } catch (IOException ex) {
                writeErr("Could not accept socket from client.\nShutting down the server thread.");
                close();
            }
        }
    }
    
    public void close(){
        
        write("Shutting down the server");
        try {
            // disconnect every client from the server
            // close all the sockets of the threads 
            for (int i=0; i<clientList.size();i++){
                if (clientList.get(i).isRunning()){
                    log.writeLn(clientList.get(i).toString());
                    clientList.get(i).send("disconnect");
                    clientList.get(i).stop();
                }
            }
            socket.close(); // close the server socket
            
            socket = null;
            clientSocket = null;
            run = false;
            clientList = null;
        } catch (IOException ex) {
            writeErr("Could not stop server. Closing forcefully");
            System.exit(-1);
        } catch (NullPointerException ex){
            writeErr("Found the server sockets were already closed. Closing forcefully.");
            System.exit(-1);
        }
        
        write("Stopped Server");
    }
    
    public int getClients(){
        int num = 0;
        for (int i=0;i<clientList.size();i++){
            if (clientList.get(i).isRunning()){
                num++;
            }
        }
        return num;
    }
    
    public void createChannel(String name, String topic){
        // check if there is a channel already with that name
        // may check that when the user asks to join the channel
        for (int i=0;i<channelList.size();i++){
            if (channelList.get(i).getChannelName() == null ? name == null : channelList.get(i).getChannelName().equals(name)){
                return;
            }
        }
        
        // create files
        FileIO channel = new FileIO("IRChat" + seperator + "conf", name + ".conf", false);
        channel.writeLn("NAME: " + name);
        channel.writeLn("TOPIC: " + topic);
        channel.writeLn("ADMINS: ");
        channel.close();
        channel = new FileIO("IRChat" + seperator + "logs", name + ".log", false);
        channel.close();
        
        // add to channel array
        channelList.add(new Channel(name, this));
        
    }
    
    public void createChannel(String name){
        createChannel(name, "");
    }
    
    private void getChannels(){
        // go through config directory to find configs for each channel
        // add them to a list of channels and create a variable for each of them
        File folder = new File("IRChat" + seperator + "conf");
        File[] files = folder.listFiles(getFilter());
        for (File file : files) {
            if (file.isFile()){
                channelList.add(new Channel(rmFilter(file.getName()), this)); // add new channel to list
            }
        }
        
        log.writeLn(channelList.size() + " channels found!");
                
    }
    
    public void setChannelAdmin(Client name, String channel){
        for (int i=0;i<channelList.size();i++){
            if (channelList.get(i).getChannelName().equals(channel)){
                channelList.get(i).setAdmin(name);
            }
        }
    }
    
    public void rmChannelAdmin(String name, String channel){
        for (int i=0;i<channelList.size();i++){
            if (channelList.get(i).getChannelName() == null ? channel == null : channelList.get(i).getChannelName().equals(channel)){
                for (int m=0;m<channelList.get(i).admin.size();m++){
                    if (channelList.get(i).admin.get(m) == null ? name == null : channelList.get(i).admin.get(m).equals(name)){
                        channelList.get(i).admin.remove(m);
                    }
                }
                return;
            }
        }
    }
    
    public void removeChannel(String name){     
        
        // remove from array
        for (int i=0;i<channelList.size();i++){
            if (channelList.get(i).getChannelName() == null ? name == null : channelList.get(i).getChannelName().equals(name)){
                log.writeLn("Channel " + name + " found!");
                channelList.get(i).rmChannel();
                channelList.remove(i);
                
                // tell all the clients to leave
                /*for (int l=0;i<channelList.get(i).channelClientList.size();l++){
                    channelList.get(i).channelClientList.get(l).send("kicked " + name);
                }*/
            }
        }     
        
    }
    
    public List<Channel> getAllChannels(){
        return channelList;
    }
    
    private FilenameFilter getFilter(){
        return new FilenameFilter() {
   
            @Override
            public boolean accept(File dir, String name) {
               if(name.lastIndexOf('.')>0)
               {
                  // get last index for '.' char
                  int lastIndex = name.lastIndexOf('.');
                  
                  // get extension
                  String str = name.substring(lastIndex);
                  
                  // match path name extension
                  if(str.equals(".conf"))
                  {
                     return true;
                  }
               }
               return false;
            }
         };
    }
    
    public String rmFilter(String s){
        if(s.lastIndexOf('.')>0)
               {
                  // get last index for '.' char
                  int lastIndex = s.lastIndexOf('.');
                  
                  // get extension
                  String str = s.substring(0,lastIndex);
                  
                  return str;
               }
               return null;
    }
    
    public int returnChannels(){
        return channelList.size();
    }
    
    public boolean isRunning(){
        return run;
    }
    
    public void write(String s){
        log.writeLn(name + ": " + s);
        log.flush();
    }
    
    public void writeErr(String s){
        log.writeErr(name + ": " + s + "\n");
        log.flush();
    }
    
    public void addUserToChannel(Client client, String channel){
        
        if (returnChannel(channel) == null){
            //log.writeLn("what happened");
            sendNotify(";There is no such channel as " + channel, client);
            
            // then maybe tell the user how to create it
            client.send("all Creating channel " + channel);
            
            // create channel
            //if (client.getLevel() < 2){
                //try {
                    createChannel(channel);
                    setChannelAdmin(client, channel);
                    addUserToChannel(client, channel);
                /*} catch (Exception e){
                    client.send("notify Channel;Could not create channel.");
                } */  
            //}
        } else {
            // check if the user is banned
            for (int i=0;i<returnChannel(channel).banned.size();i++){
                if (client.getName() == returnChannel(channel).banned.get(i)){
                    client.send("notify Banned;You are banned from the channel");
                    return;
                }
            }            
            
            // check if there is already a user with this nickname in the room
            for (int i=0;i<returnChannel(channel).channelClientList.size();i++){
                if (client.getNickname() == null ? returnChannel(channel).channelClientList.get(i).getNickname() == null : client.getNickname().equals(returnChannel(channel).channelClientList.get(i).getNickname())){
                    // they are already connected
                    client.send("notify " + channel+";You are already connected to this channel.");
                    return;
                }
            }
            
            
            send("createchan " + channel, client.getName());
            for (int i=0;i<channelList.size();i++){
                if (channelList.get(i).getChannelName().equals(channel)){
                    channelList.get(i).addUser(client);
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException ex) {
                        java.util.logging.Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (!"".equals(channelList.get(i).getTopic().trim())){
                        send("topic " + channel +  " Topic: " + channelList.get(i).getTopic(), client.getName());
                    }
                    
                    // check if the user is an admin
                    if (channelList.get(i).isAdmin(client.getName())){
                        
                    }
                    
                    
                    return;
                }
            }
            
        }
        
    }
    
    public void send(String msg, String client){
        returnClient(client).send(msg);
    }
    
    public void removeUserFromChannel(Client client, String channel){
        int index = -1;
        for (int i=0;i<channelList.size();i++){
            if (channelList.get(i).getChannelName().equals(channel)){
                index = i;
            }
        }
        
        if (index == -1){
            log.writeErr("Could not find a channel with this name");
            return;
        }
        
        for (int i=0;i<channelList.get(index).channelClientList.size();i++){
            if (channelList.get(index).channelClientList.get(i).equals(client)){
                channelList.get(index).sendToUser("You have been disconnected from this channel", client.getNickname());
                channelList.get(index).channelClientList.remove(i);
            }
        }
        
    }
    
    public void sendToChannel(String channel, String nickname, String msg){
        for (int i=0; i<channelList.size();i++){
            if (channelList.get(i).getChannelName().equals(channel)){
                //log.writeLn("[" + nickname + "]: " + msg);
                channelList.get(i).send("[" + nickname + "]: " + msg);
            }
        }
    }
    
    public void notifyChannel(String channel, String msg){
        
        returnChannel(channel).sendNotify(msg);
        
    }
    
    public void sendNotify(String msg, Client client){
        client.send("notify " + msg);
    }
    
    public void sendNotifyToAll(String msg){
        for (int i=0;i<clientList.size();i++){
            clientList.get(i).send("notify " + msg);
        }
    }
    
    public Channel returnChannel(String channel){
        for (int i=0;i<channelList.size();i++){
            if (channelList.get(i).getChannelName() == null ? channel == null : channelList.get(i).getChannelName().equals(channel)){
                return channelList.get(i);
            }
        }
        return null;
    }
    
    public Client returnClient(String name){
        for (int i=0;i<clientList.size();i++){
            if (clientList.get(i).getName().equals(name)){
                return clientList.get(i);
            }
        }
        return null;
    }
    
    public Client retClient(String name){
        for (int i=0;i<clientList.size();i++){
            if (clientList.get(i).getNickname().equals(name)){
                return clientList.get(i);
            }
        }
        return null;
    }
    
    public void sendToAll(String msg, String nickname){
        for (int i=0;i<clientList.size();i++){
            if (clientList.get(i).getNickname() == null ? nickname != null : !clientList.get(i).getNickname().equals(nickname)){
                clientList.get(i).send("msg [" + nickname + "]: " + msg);
            }
        }
    }
    
    public void sendCommand(String command){
        for (int i=0;i<clientList.size();i++){
            clientList.get(i).send(command);
        }
    }
    
    public void sendPrivMessage(String msg, String nickname, String from){
        returnClient(nickname).send("pmsg " + from + " " + msg);
    }
    
    
    public void addBan(String name){
        if (!"".equals(name.trim())){
            bannedList.add(name.trim());
            
            returnClient(name).send("disconnect"); // tell the user to get off
            
            returnClient(name).stop(); 

            
        }
    }
    
    public void rmBan(String name){
        if (!"".equals(name.trim())){
            bannedList.remove(name.trim());
        }
    }
    
    public boolean isAdmin(String loggedinname, String channel){ // needs to be logged in name instead of name
        return returnChannel(channel).isAdmin(loggedinname);
    }
    
    
}
