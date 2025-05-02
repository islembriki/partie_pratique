import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Stack;
import java.io.*;
import java.net.*;
import java.util.Scanner;
public class client {
    private static DatagramSocket socket;
    private static InetAddress serverAddress;
    private static int server_port = 5678; // Port for network simulator
    private static String server_ip = "localhost";
    public static void main(String[] args) {
        final int NBOUTONS = 20;
        JButton[] boutons;
        JTextField txt;
        JPanel pan;
        JFrame frame = new JFrame("Calculatrice");
        frame.setSize(300, 400);
        Container contenu = frame.getContentPane();
        contenu.setLayout(new BorderLayout(10, 20));
        contenu.setBackground(new Color(42, 75, 124)); 
        pan = new JPanel();
        pan.setBackground(new Color(42, 75, 124)); 
        txt = new JTextField();
        txt.setFont(new Font("Thaoma", Font.PLAIN, 20));
        txt.setHorizontalAlignment(JTextField.RIGHT);
        txt.setPreferredSize(new Dimension(0, 40));
        JPanel txtWrapper = new JPanel(new BorderLayout());
        txtWrapper.setBorder(new EmptyBorder(10, 10, 0, 10)); 
        txtWrapper.add(txt, BorderLayout.CENTER);
        txtWrapper.setBackground(new Color(42, 75, 124)); 
        contenu.add(txtWrapper, BorderLayout.NORTH);
        pan.setLayout(new GridLayout(5, 4, 5, 5));
        boutons = new JButton[NBOUTONS];
        String[] b = {"C","±","%","÷","7","8","9","x","4","5","6","-","1","2","3","+","0",".","DEL","="};
        for (int i = 0; i < NBOUTONS; i++) {
            boutons[i] = new JButton(b[i]);
            boutons[i].setFont(new Font("Arial", Font.BOLD, 16));
            boutons[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            boutons[i].setFocusPainted(true);
            if (i >= 4 && i <= 6 || i >= 8 && i <= 10 || i >= 12 && i <= 14 || i == 16) {
                boutons[i].setBackground(new Color(255, 180, 76)); 
                boutons[i].setForeground(Color.BLACK);
            } else {
                
                boutons[i].setBackground(Color.LIGHT_GRAY);
                boutons[i].setForeground(Color.BLACK);
            }
            
            if (i == 19) { 
                boutons[i].setBackground(new Color(169, 169, 169)); 
            }

            pan.add(boutons[i]);
        }
        JPanel panWrapper = new JPanel(new BorderLayout());
        panWrapper.setBorder(new EmptyBorder(0, 10, 10, 10));
        panWrapper.add(pan, BorderLayout.CENTER);
        panWrapper.setBackground(new Color(42, 75, 124));
        contenu.add(panWrapper, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(server_ip);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Socket init failed: " + e.getMessage());
        }
        ActionListener listener1 = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton source = (JButton) e.getSource();
                String text = source.getText();
                
                if ("+x÷%-".contains(text)) {
                    if (txt.getText().length() > 0) {
                        txt.setText(txt.getText() + text);
                    }
                } else if ("C".equals(text)) {
                    txt.setText("");
                    txt.setForeground(Color.BLACK); 
                } else if ("=".equals(text)) {
                    
                    String expression = txt.getText();
                    
                    // Disable the text field while processing
                    txt.setEnabled(false);
                    
                    // Use SwingWorker to perform network operations in background
                    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                        @Override
                        protected String doInBackground() throws Exception {
                            try {
                                sendToServer(expression, serverAddress, server_port);
                                return receiveFromServer();
                            } catch (Exception ex) {
                                return "Erreur: " + ex.getMessage();
                                
                            }
                        }
                        @Override
                        protected void done() {
                            try {
                                String result = get();
                                if (result.startsWith("Erreur:")) {
                                    txt.setText(result);
                                    txt.setForeground(Color.RED);
                                } else {
                                    txt.setText(result);
                                    txt.setForeground(Color.BLACK);
                                }
                            } catch (Exception e) {
                                txt.setText("Erreur: " + e.getMessage());
                                txt.setForeground(Color.RED);
                            } finally {
                                txt.setEnabled(true);
                            }
                        }
                    };
                    worker.execute();
                } else if (".".equals(text)) {
                    if (txt.getText().length() > 0 && !txt.getText().endsWith(".")) {
                        // Check if the current number already has a decimal point
                        String currentText = txt.getText();
                        int lastOperatorIndex = Math.max(
                            Math.max(currentText.lastIndexOf('+'), currentText.lastIndexOf('-')),
                            Math.max(currentText.lastIndexOf('x'), currentText.lastIndexOf('÷'))
                        );
                        
                        String currentNumber = currentText.substring(lastOperatorIndex + 1);
                        if (!currentNumber.contains(".")) {
                            txt.setText(currentText + text);
                        }
                    }
                } else if ("±".equals(text)) {
                    String currentText = txt.getText();
                    if (currentText.length() > 0) {
                        if (currentText.startsWith("-")) {
                            txt.setText(currentText.substring(1));
                        } else {
                            txt.setText("-" + currentText);
                        }
                    }
                } else if ("DEL".equals(text)) {
                    String currentText = txt.getText();
                    if (currentText.length() > 0) {
                        txt.setText(currentText.substring(0, currentText.length() - 1));
                    }
                } else {
                    txt.setText(txt.getText() + text);
                }
            }
        };
        for (int i = 0; i < NBOUTONS; i++) {
            boutons[i].addActionListener(listener1);
        }
    }
   private static void sendToServer(String message, InetAddress address, int port) throws IOException {
    // Calculate checksum for message
    int checksum = calculateChecksum(message);
    // Combine message and checksum
    String messageWithChecksum = message + "|" + checksum;
    String sending=messageWithChecksum.trim();
    byte[] buf = sending.getBytes();//thwlk li bytes li des octets , ken tiksl a2 bytes koulch alia ksmt ala 16bits -->hexadecima
    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
    socket.send(packet); 
}
   /* private static int calculateChecksum(String message) {
        // Simple checksum: sum of character values
        int sum = 0;
        for (char c : message.toCharArray()) {
            sum += c;
        }
        return sum;
    }*/
// hedi bi bytes 
/*
 * private static int calculateChecksum(String data) {
    // Convert data to bytes
    byte[] byteArray = data.getBytes();
    
    // Sum variable to accumulate the checksum
    int sum = 0;
    
    // Process the byte array in 2-byte chunks
    for (int i = 0; i < byteArray.length; i += 2) {
        // Get the 16-bit word (2 bytes)
        int word = byteArray[i] & 0xFF; // Get the first byte
        if (i + 1 < byteArray.length) {
            word = (word << 8) | (byteArray[i + 1] & 0xFF); // Combine with the second byte
        }
        
        // Add the 16-bit word to the sum
        sum += word;
        
        // If sum overflows, wrap around the overflow (carry)
        if ((sum & 0xFFFF) < word) {
            sum++; // Carry the overflow back into the sum
        }
    }
    
    // Now apply one's complement (invert all bits)
    sum = ~sum & 0xFFFF; // Only keep the lower 16 bits
    
    return sum; // This is the checksum
}

 */

//methode maakda taa checksum 
    private static int calculateChecksum(String message) {
        // Define the word size in bits
        final int WORD_SIZE = 16; // 16-bit words
        final int CHECKSUM_SIZE = 16; // Final checksum will be 16 bits
        
        // Convert message to bit representation
        StringBuilder bitRepresentation = new StringBuilder();
        for (char c : message.toCharArray()) {
            // Convert each character to 16-bit binary representation
            String binaryChar = Integer.toBinaryString(c);
            // Pad to ensure 16 bits per character
            while (binaryChar.length() < 16) {
                binaryChar = "0" + binaryChar;
            }
            bitRepresentation.append(binaryChar);
        }
        // Ensure the bit representation length is a multiple of WORD_SIZE
        // by padding with zeros if necessary
        while (bitRepresentation.length() % WORD_SIZE != 0) {
            bitRepresentation.append("0");
        }
        // Divide the bits into words and sum them
        int sum = 0;
        for (int i = 0; i < bitRepresentation.length(); i += WORD_SIZE) {
            // Extract a word of WORD_SIZE bits
            String word = bitRepresentation.substring(i, Math.min(i + WORD_SIZE, bitRepresentation.length()));
            // Convert the binary word to an integer and add to sum
            int wordValue = Integer.parseInt(word, 2);
            sum += wordValue;
        }
        // Take only the least significant CHECKSUM_SIZE bits
        // This is done by using a bitmask: (1 << CHECKSUM_SIZE) - 1
        int checksum = sum & ((1 << CHECKSUM_SIZE) - 1);
        // Apply one's complement to the checksum
        checksum = ~checksum & ((1 << CHECKSUM_SIZE) - 1); // Invert bits and keep only CHECKSUM_SIZE bits
        return checksum;
    }

    private static String receiveFromServer() throws IOException {
        // Set timeout to 2 seconds (more reasonable than 1 second)
        socket.setSoTimeout(2000);
        
        byte[] buffer = new byte[4096];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        try {
            socket.receive(packet);
            String result = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received from server: " + result);
            return result;
        } catch (SocketTimeoutException e) {
            System.out.println("Socket timeout occurred");
            
            // Try one more time before giving up
            try {
                System.out.println("Attempting second receive...");
                socket.receive(packet);
                String result = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received on second attempt: " + result);
                return result;
            } catch (Exception ex) {
                System.out.println("Second receive attempt failed: " + ex.getMessage());
                // Return appropriate error message
                return "Erreur: Serveur ne répond pas";
            }
        } finally {
            // Reset timeout for future operations
            socket.setSoTimeout(0);
        }
    }
}