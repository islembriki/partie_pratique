//classe hedi hia traffic , hia fin chance ghatla fin chtsir 
import java.io.*;
import java.net.*;
import java.util.Stack;
public class networksimulator {
    private static DatagramSocket clientSocket;
    private static DatagramSocket serverSocket;
    private static double errorRate = 0.2; // 50% chance of error
    public static void main(String[] args) throws Exception {
        clientSocket = new DatagramSocket(5678); // Listen for client
        serverSocket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName("localhost");
        int serverPort = 1234;
        System.out.println("Network simulator running...");
        while (true) {
            // Receive from client
            byte[] buffer = new byte[1024];
            DatagramPacket clientPacket = new DatagramPacket(buffer, buffer.length);
            clientSocket.receive(clientPacket);
            String data = new String(clientPacket.getData(), 0, clientPacket.getLength());
            System.out.println("Received from client: " + data);

            if (Math.random() < errorRate) {
                data = introduceError(data);
                System.out.println("Introduced error, new data: " + data);
            }

            byte[] modifiedData = data.getBytes();
            DatagramPacket serverPacket = new DatagramPacket(
                modifiedData, modifiedData.length, serverAddress, serverPort); serverSocket.send(serverPacket);
            
            // Receive response from server
            buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(responsePacket);
            
            // Forward response back to client
            DatagramPacket clientResponsePacket = new DatagramPacket(
                responsePacket.getData(), 
                responsePacket.getLength(),
                clientPacket.getAddress(),
                clientPacket.getPort());
            clientSocket.send(clientResponsePacket);
        }
    }
    //hne ki aawdt thabt lkit inha tnjm taaml error hata fi checksum bidou donc hani amlt introduce error okhra tbdlch fi checksum
    //w kolt nkhtarou ken tbuha hata mi checksum taaml fih ghalta w ila le 
    //hedi taaml ghalta fi checksum  
    /*
    private static String introduceError(String data) {
        // Simple error: change a random character
        char[] chars = data.toCharArray();
        if (chars.length > 0) {
            int pos = (int)(Math.random() * chars.length);
            chars[pos] = (char)(chars[pos] + 1); // Change one character
        }
        return new String(chars);
    } */

    
   //hedi taamlch ghlta , ken fi donne taaml:
   private static String introduceError(String data) {
    // Split the message and checksum based on the '|' separator
    String[] parts = data.split("\\|");
    
    if (parts.length > 1) {
        String message = parts[0];  // The part before the '|'
        String checksum = parts[1]; // The part after the '|'
        
        // Introduce error in the message part
        char[] messageChars = message.toCharArray();
        if (messageChars.length > 0) {
            int pos = (int)(Math.random() * messageChars.length); // Random position in the message part
            messageChars[pos] = (char)(messageChars[pos] + 1);  // Modify one character
        }
        
        // Recombine the message and checksum
        return new String(messageChars) + "|" + checksum;
    } else {
        // If no checksum exists, just return the original data
        return data;
    }
}
}