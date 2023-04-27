package Encoder;

import java.io.BufferedReader;
//import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * @author Jim McKeown
 */

public class EncServer implements Runnable
{
    private int serverSocket = 0;
    private boolean okToRun = true;
    private ServerSocket encServer = null;
    private BufferedReader in = null;
    private PrintWriter out = null;
    //private DataOutputStream os = null;
    //private boolean isConnected = false;
    private Socket clientSocket = null;
    private Encoder enc;
    
    public EncServer(Encoder enc, int serverSocket)
    {
        this.enc = enc;
        this.serverSocket = serverSocket;
    }
    
    public boolean isConnected()
    {
        boolean result = false;
        if(clientSocket == null)
        {
            result = false;
        }
        else
        {
            result = true;
        }
        return result;
    }
    public void quit()
    {
        okToRun = false;
    }
    
    //@Override
    public void run() 
    {
        String[] inArray;
        //setup socket communication
        try
        {
            encServer = new ServerSocket(serverSocket);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
        String input;

        while(okToRun)
        {
            clientSocket = null;
            try
            {
                System.out.println("Waiting for client to connect on server "
                        + "socket " + serverSocket);
                clientSocket = encServer.accept();
                System.out.println("Client connected.");
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                while (true) 
                {
                    input = in.readLine();
                    inArray = input.split(",");
                    if(inArray.length == 1)
                    {
                        if(inArray[0].equals("A"))
                        {
                            out.println(enc.getAngle());
                        }
                        if(inArray[0].equals("T"))
                        {
                            out.println(enc.getThreshold());
                        }
                        if(inArray[0].equals("S"))
                        {
                            out.println(enc.getSize());
                        } 
                        if (input.equals("Q")) 
                        {
                            break;
                        }                                 
                    }
                    if(inArray.length == 2)
                    {
                        if(inArray[0].equals("D"))
                        {
                            if (inArray[1].equals("A")) 
                            {
                                enc.setDisplay(1);
                                out.println("A");
                            }
                            if (inArray[1].equals("D")) 
                            {
                                enc.setDisplay(2);
                                out.println("D");
                            }
                            if (inArray[1].equals("N")) 
                            {
                                enc.setDisplay(0);
                                out.println("N");
                            }
                        }
                        if(inArray[0].equals("T"))
                        {
                            //enc.setThreshold((int)Integer.valueOf(inArray[1]));
                            enc.setThreshold( (Integer.valueOf(inArray[1])).intValue());
                            out.println("T," + enc.getThreshold());
                        }
                        if(inArray[0].equals("S"))
                        {
                            //enc.setSize((int)Integer.valueOf(inArray[1]));
                            enc.setSize((Integer.valueOf(inArray[1])).intValue());
                            out.println("S," + enc.getSize());
                        }                                    
                    }
                }
                clientSocket = null;
            }
            catch(IOException e)
            {
                System.out.println(e);
            }
            catch(NullPointerException npe)
            {
                clientSocket = null;
            }
        }
    }
}
