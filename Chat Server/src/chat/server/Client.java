

package chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;


public class Client {
    
    private Socket socket;
    private Thread thread;
    private clientThread client;
    
    private String ipaddress;
    private String nickname;
    private int level;
    private String loggedName;
    private String computerName;
    private int index;
    
    private String[] channels = new String[5];
    
    // reading and writing to socket
    PrintWriter out;
    BufferedReader in;
    Logger log;
    
    ServerThread handler;
    
    public Client(Socket soc, int i, ServerThread h){
        socket = soc;
        index = i;
        level = 1;
        
        handler = h;
        
        log = new Logger();
        
        try {         
            // create input/output streams
            
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

        } catch (IOException ex) {
            log.writeErr("Could not create client input/output streams " + i);
        }
        
        out.println("a"); // response to being accepted
        
        client = new clientThread();
        thread = new Thread(client);
        thread.start();
    }
    
    public void send(String s){
        out.println(s);
    }
    
    public int getIndex(){
        return index;
    }
    
    public void setIndex(int num){
        index = num;
    }
    
    public boolean isRunning(){
        return socket != null;
    }
    
    public String getName(){
        return loggedName;
    }
    
    public String getNickname(){
        return nickname;
    }
    
    public void stop(){
        
        try {
            
            // remove from each channel
            //for (int i = 0; i<channels.length;i++){
            //    handler.removeUserFromChannel(nickname, handler.returnChannel(channels[i]).getChannelName());
            //}
            
            
            computerName = "";
            ipaddress = "";
            loggedName = "";
            nickname = "";
            
            in.close();
            out.close();
            socket.close();
            
            log.writeLn("Closing socket");
            socket = null; // to show it is not runing
            thread.stop();
            
        } catch (IOException ex) {
            log.writeErr("Error stopping client thread: " + index);
        } finally {
            thread.stop();
        }
        
    }
    
    /* IMPORTANT: this parses every command that is sent in from the clients */
    private void processCommand(String command){
        // check command to see if it asks anything of the server
        
        // if the name is at an admin level
        if (this.level == 0){
            if (command.startsWith("//")){
                log.writeLn("Recieved admin command from: " + this.getName() + ", " + command);
                String[] com = command.substring(2).toLowerCase().split(" ");
                
                switch (com[0]){
                    case "addban": // ban user
                        try {
                            send("all Banning " + handler.retClient(com[1]).getNickname() + " who is " + handler.retClient(com[1]).getName());
                            
                            log.writeLn("Banning " + handler.retClient(com[1]).getNickname() + " who is " + handler.retClient(com[1]).getName());
                            
                            handler.addBan(handler.retClient(com[1]).getName());
                            
                        } catch (Exception e){
                            send("all Could not find the user to ban");
                        }
                        break;
                    case "rmban":
                        try {
                            send("all Removing ban" + com[1]);
                            
                            log.writeLn("Removing ban " + com[1]);
                            handler.rmBan(com[2]);
                        } catch (Exception e){
                            send("all Could not find the user to unban");
                        }
                        break;
                        case "addbann": // ban user
                        try {
                            send("all Banning " + handler.returnClient(com[1]).getNickname() + " who is " + handler.returnClient(com[1]).getName());
                            
                            log.writeLn("Banning " + handler.returnClient(com[1]).getNickname() + " who is " + handler.returnClient(com[1]).getName());
                            handler.addBan(handler.returnClient(com[1]).getName());
                        } catch (Exception e){
                            send("all Could not find the user to ban");
                        }
                        break;
                    case "help":
                        send("all addban and rmban to ban people from the server");
                        send("all If you add an 'n' to the end of the command it will search for the logged in name instead of nickname");
                        send("all There is also a notify command and kick");
                        break;
                    case "notify":
                        //log.writeLn(command);
                        String[] temp = command.substring(9).split(";");
                        try {
                            handler.sendCommand("notify " + temp[0] + ";" + temp[1]);
                            log.writeLn(temp[0] + " " + temp[1]);
                        } catch (Exception e){
                            log.writeErr("notify takes two(2) that are seperated by a ';' character");
                        }
                        break;
                    case "kick":
                        
                        break;
                    case "restart":
                        handler.server.restart();
                    default:
                        send("notify ;That was not a recognized command by the server.");
                    
                }
                
                return;
            }
            
        }
        
        
        String[] com = command.split(" ");
        switch (com[0]) {
            case "info":
                // process the info that is sent and send to command line
                try {
                    computerName = com[1].toLowerCase();
                    ipaddress = com[2].toLowerCase();
                    
                    // find if there is already a client associated with an account
                    for (int i=0;i<ServerThread.clientList.size();i++){
                        if (ServerThread.clientList.get(i).getName() == null ? com[3].toLowerCase() == null : ServerThread.clientList.get(i).getName().equals(com[3].toLowerCase())){
                            
                            send("disconnect");
                            this.stop();
                        }
                    }
                    loggedName = com[3].toLowerCase(); // then set logged in name
                    
                    // if name is mackh automatically make server admin
                    if ("mackh".equals(loggedName)){
                        level = 0;
                        send("admin true");
                        log.writeLn("Found admin!");
                    } else {
                        level = 1;
                    }
                    
                    // check if nickname is already used.
                    String tempNickname = com[4].toLowerCase();
                    
                    boolean change = false;
                    for (int i=0;i<ServerThread.clientList.size();i++){
                        if (ServerThread.clientList.get(i).getNickname() == null ? tempNickname == null : ServerThread.clientList.get(i).getNickname().equals(tempNickname)){
                            change = true;
                        }
                    }
                    if (change){
                        send("notify The username " + tempNickname + " is already taken.");
                        nickname = "";
                        send("requestnick");
                        
                    } else {
                        nickname = tempNickname;
                    }
                    
                    
                    // check if they are blocked from the server
                    for (int i=0;i<ServerThread.bannedList.size();i++){
                        if (loggedName.equals(ServerThread.bannedList.get(i))){
                            send("notify Banned;You have been banned from the server.");
                            send("disconnect");
                            stop();
                        }
                    }
                    
                    
                } catch (Exception e) {
                    log.writeLn(index + ": sent invalid data. disconnecting now");
                    send("disconnect");
                    stop();
                    return;
                }   log.writeLn("Connected: index[" + index +"] " + computerName + ";" + ipaddress + ";" + loggedName + ";" + nickname + ";");
            
            // at this point should check if user is banned,
            
            // if so send them a message
            // to disconnect them, otherwise continue
                break;
            case "nick":
                // check to see if the username is already used
                String s;
                try {
                    s = com[1].toLowerCase();
                } catch (Exception e){
                    return;
                }   boolean change = true;
            for (int i=0;i<ServerThread.clientList.size();i++){
                if (ServerThread.clientList.get(i).getNickname() == null ? s == null : ServerThread.clientList.get(i).getNickname().equals(s)){
                    change = false;
                }
            }   if (change){
                nickname = s;
                send("nick " + nickname);
            } else {
                send("notify ;The username " + s + " is already taken.");
            }   break;
            case "msg":
                //handler.sendToAll(command.substring(4), this.nickname);
                //log.writeLn(com[1] + ";" + nickname + ";" + command.substring(5 + com[1].length()));
                handler.sendToChannel(com[1], nickname, command.substring(5 + com[1].length()));
                break;
            case "pmsg":
                try {
                String name = com[1].toLowerCase();
                com[1] = this.nickname;
                String temp = "";
                
                temp += com[0] + " " + com[1] + " ";
                temp += "[" + com[1] + "]: " + command.substring(6 + name.length());
                /*for (String t : com){
                    temp += t + " ";
                }*/
                handler.retClient(name).send(temp.trim());
                } catch (Exception e){
                    send("There is no user with this name.");
                }
                break;
            case "join":
                try {
                    handler.addUserToChannel(this, com[1]); // check if exists..
                    // if not create channel and add this user to admin list
                    // then notify client
                    log.writeLn("adding " + nickname + " to channel " + com[1]);
                } catch (Exception e){
                    send("notify ;Channel name cannot be empty");
                }
                break;
            case "leave":
                handler.removeUserFromChannel(this, com[1]);
                log.writeLn("removing " + nickname + " from channel " + com[1]);
                break;
            case "checkVersion":
                try {
                    if (ChatServer.clientVersion == null ? com[1] != null : !ChatServer.clientVersion.equals(com[1])){
                        send("notify Version;You are using an outdated version of Jungle Chat");
                    }
                } catch (Exception e){}
                break;
            case "requestlist":
                List<Channel> list = handler.getAllChannels();
                //String temp = "list " + com[1] + " ";
                //send("list " + com[1] + " List of channels:");
                for (int i=0;i<list.size();i++){
                    send("list " + com[1] + " Name: " + list.get(i).getChannelName() + " Topic: " + list.get(i).getTopic() + " ");
                }
                //send(temp);
                break;
            case "getusers":
                try {
                    send("users " + com[1] + " " + handler.returnChannel(com[1]).sendClients());
                } catch (Exception e){
                    
                }   
                break;
            case "whois":
                // find information about the user and send to client
                try {
                    Client t = handler.retClient(com[1].toLowerCase());
                    send("whois Information about: " + t.nickname);
                    Thread.sleep(10);
                    send("Computer name: " + t.computerName + "\nIP address: " + t.ipaddress + "\nName: " + t.loggedName + "\nEnd of information");
                    
                } catch (Exception e){
                    send("all Could not find the client you requested.");
                    log.writeLn(e.getMessage());
                }
                break;
            case "newtopic":
                try {
                    if (handler.isAdmin(loggedName, com[1]) || level == 0){
                        handler.returnChannel(com[1]).setTopic(command.substring(10 + com[1].length()));
                        // send the updated topic to everyone on the channel
                        handler.returnChannel(com[1]).sendPure("topic " + com[1] + " Topic: " + handler.returnChannel(com[1]).getTopic());
                    
                    } else {
                        send("notify Admin;You are not an admin of this channel");
                    }
                    
                } catch (Exception e){
                    send("all Could not find the channel.");
                }
                break;
            case "addban":
                try {
                    if (handler.isAdmin(loggedName, com[1]) || level == 0){
                        handler.returnChannel(com[1]).addBan(handler.retClient(com[2]).getName());
                        send("all Successfully added ban");
                    } else {
                        send("notify Admin;You are not an admin of this channel");
                    }
                    
                } catch (Exception e){
                    send("all Could not find the channel.");
                }
                break;
            case "rmban":
                try {
                    if (handler.isAdmin(loggedName, com[1]) || level == 0){
                        handler.returnChannel(com[1]).rmBan(handler.retClient(com[2]).getName());
                        send("all Successfully removed ban");
                    } else {
                        send("notify Admin;You are not an admin of this channel");
                    }
                    
                } catch (Exception e){
                    send("all Could not find the channel.");
                }
                break;
            case "banned":
                try {
                    if (handler.isAdmin(loggedName, com[1]) || level == 0){
                        String names = "";
                        Channel chan = handler.returnChannel(com[1]);
                        for (int i=0;i<chan.banned.size();i++){
                            names += handler.returnClient(chan.banned.get(i)).getNickname() + "\n";
                        }
                        
                        send("all Banned:\n" + names);
                    } else {
                        send("notify Admin;You are not an admin of this channel");
                    }
                    
                } catch (Exception e){
                    send("all Could not find the channel.");
                }
                break;
            case "kick":
                try {
                    if (handler.isAdmin(loggedName, com[1]) || level == 0){
                        log.writeLn("Kicking " + com[2] + " from " + com[1]);
                        handler.returnChannel(com[1]).rmUser(com[2]);
                        handler.retClient(com[2]).send("kicked " + com[1]);
                        send("all Kicked " + com[2]);
                    } else {
                        send("notify Admin;You are not an admin of this channel");
                    }
                    
                } catch (Exception e){
                    send("all Could not find the channel.");
                }
                break;
            case "rmchannel":
                try {
                    if (handler.isAdmin(loggedName, com[1]) || level == 0){
                        handler.removeChannel(com[1].trim());
                        send("all Closing Channel");
                    } else {
                        send("notify Admin;You are not an admin of this channel");
                    }
                    
                } catch (Exception e){
                    send("all Could not find the channel.");
                }
                break;
            default:
                if (this.loggedName != null)
                    log.writeErr(this.loggedName + " has sent us a strange command?");
                else
                    log.writeErr(this.index + " has sent us a strange command?\n" + command);
                break;
        }
        
    }
    
    public int getLevel(){
        return level;
    }
    
    private class clientThread implements Runnable{
        @Override
        public void run() {
            try {
                
                // read messages sent in from this client
                String s;
                while (!"disconnect".equals(s = in.readLine())){
                   if (s != null){
                       //log.writeLn(index + ": " + s);
                       processCommand(s);
                       
                   }
                }
                
                send("disconnect"); // if dont want them to disconnect send("hold");
                
                log.writeLn("Stopping " + index);
                stop();
            } catch (IOException ex) {
                log.writeErr("Could not read from socket: " + index);
                stop();
            }
        }
    
    }
}