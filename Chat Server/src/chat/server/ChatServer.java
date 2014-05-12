////////////////////////////
// port : 2226
////////////////////////////

package chat.server;

import java.util.logging.Level;

public class ChatServer {
    
    static Logger log = new Logger();
    
    public static final String version = "1.0.0.0";
    public static final String clientVersion = "1.0.0.0";
    
    // variables
    static String username;
    static int permissions;
    ServerThread server;
    Thread serverT;
    
    
    public static void main(String[] args) {
        
        
        
        // parse command line arguments
        for (String arg : args) {
            if ("-s".equals(arg)){ // skip the login
                username = "root";
                permissions = 0;
            }
        }
        
        // temporary!!!
        username = "root";
        permissions = 0;
        // end temp
        
        log.writeLn("\n############################");
        log.writeLn("#       Chat Server        #");
        log.writeLn("#    Version: " + version + "      #");
        log.writeLn("############################\n");
        
        // init class
        ChatServer chat = new ChatServer();
        // set up commands
        
        if (username == null){
            for (int i=1; i<4;i++){
                if (i != 1){
                    log.writeLn("You have " + (4-i) + " chances left.");
                }
                if (chat.login()){
                    i=4;
                }
            }

            // exit program if username is wrong
            if (username == null) {
                log.writeErr("You have run out of chances.");
                System.exit(-1);
            }
        }
        
        // login success!!!
        log.writeLn("----------------------------\n");
        
        // either start set up the server and start that thread
        // or ask the user to type a command to start the server
        chat.startUp();
        
        while (!chat.server.isRunning()){
            log.flush();
        }
        
        chat.looper(); // start the command loop

        //while (chat.server.isRunning()){}
        
        chat.shutDown();
        log.writeLn("");
    }

    public ChatServer() {
        
    }
    
    public boolean login(){
        // next ask for the username and password for the server
        log.write("Log in as? ");
        
        String temp = log.readLn();
        
        // Search text file to find entry with that username
        // then ask for password
        // if correct set username and permission values
        // at the moment it only checks against inbuilt username and password
        
        log.write(temp + "'s password: ");
        // flush the output
        log.flush();
        
        // mask the input
        String pass = log.readPassword();
        pass = pass.substring(0);
        
        log.flush();
        
        if ("root".equals(temp) && "toor".equals(pass)){
            username = "root";
            permissions = 0;
            pass = ""; // remove the password
            return true;
        } else {
            log.writeLn("\rThe password is incorrect.\n");
            return false;
        }
        
    }
    
    // start the program
    public void startUp(){
        log.writeLn("Welcome, " + username);
        log.writeLn("Please wait while we bind the server socket");
        log.writeLn("Starting all threads");
        
        // start the server thread
        server = new ServerThread(this);
        serverT = new Thread(server, "Client Accept");
        serverT.start(); // start it running
        
    }
    
    // shutdown the program
    public void shutDown(){
        server.close();
        serverT.stop();
        log.writeLn("Stopped all threads");
        
        // make sure there is nothing there.
        server = null;
        serverT = null;
        
    }
    
    // this is the command line
    public void looper(){
        boolean run = true;        
        while (run){
            try {
                log.write(username + "?>");
                String command = log.readLn();
                if (!parseCommand(command)){
                    run = false;
                }
            } catch (Exception e){
                
            }
        }
    }
    
    // to add commands add here >>>>>
    public boolean parseCommand(String command){
        //log.writeLn(command);
        String[] com = command.split(" "); // split command

        switch (com[0]){
            case "exit":
                return false;
            case "stop":
                if (server.isRunning()){
                    shutDown();
                } else {
                    log.writeLn("The server is already stopped");
                }
                break;
            case "start":
                if (!server.isRunning()){
                    startUp();
                } else {
                    log.writeLn("The server is already started");
                }
                break;
            case "help":
                break;
            case "clients":
                log.writeLn("Clients connected: " + server.getClients());
                break;        
            case "info":
                log.getProperties();
                break; 
            case "rmchannel":
                try {
                    server.removeChannel(com[1].trim());
                } catch (Exception e){
                    log.writeErr("Need a channel name");
                }
                break; 
            case "createchannel":
                try {
                    if (com[1] != null){
                        String temp = "";
                        for (int i=2;i<com.length;i++){
                            if (i == 2){
                                temp = com[i];
                            } else {
                                temp = temp + " " + com[i];
                            }
                        }
                        if (temp != ""){
                            server.createChannel(com[1], temp);
                        } else {
                            server.createChannel(com[1]);
                        }
                    }
                } catch (Exception e){
                    log.writeErr("Need name and topic to create channel.");
                }
                break; 
            case "channels":
                log.writeLn("There are " + server.returnChannels() + " channels");
                for (int i = 0; i < ServerThread.channelList.size(); i++){
                    log.writeLn("Name: " + ServerThread.channelList.get(i).getChannelName() + " Topic: " + ServerThread.channelList.get(i).getTopic());
                }
                break; 
            case "notify":
                //log.writeLn(command);
                String[] temp = command.substring(7).split(";");
                try {
                    server.sendCommand("notify " + temp[0] + ";" + temp[1]);
                    log.writeLn(temp[0] + " " + temp[1]);
                } catch (Exception e){
                    log.writeErr("notify takes two(2) that are seperated by a ';' character");
                }
                break;
            case "addadmin":
                
                try {
                    log.writeLn("Adding admin: " + com[1] + " to " + com[2]);
                    for (int i=0;i<ServerThread.clientList.size();i++){
                        if (ServerThread.clientList.get(i).getNickname() == null ? com[1] == null : ServerThread.clientList.get(i).getNickname().equals(com[1])){
                            server.setChannelAdmin(ServerThread.clientList.get(i), com[2]);
                            log.writeLn("success");
                        }
                    }
                } catch (Exception e){
                    log.writeErr("addadmin [nickname] [channel]");
                }
                
                break;
            case "restart":
                restart();
                break;
            default:
                log.writeErr("That command could not be found.");
                break;
        }
        return true;
    }
    
    public void restart(){
        
        try {
            shutDown();
            
            log.writeLn("\n======================================================");
            Thread.sleep(5000);
            
            startUp();
        } catch (InterruptedException ex) {
            
        }
    }
    
}
