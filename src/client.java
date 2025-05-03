//importer les libraries nécessaires pour les components GUI et la communication réseau
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
//Importer les bibliothèques d'E/S et de réseau pour la communication UDP
import java.io.*;
import java.net.*;
public class client {
    private static DatagramSocket socket;// UDP socket pour la communication
    private static InetAddress serverAddress;// adresse ip du serveur pour l'envoie des packets'
    private static int server_port = 5678; // Port pour le simulateur de réseau
    private static String server_ip = "localhost";//addresse IP du serveur, localhost pour le test local
    public static void main(String[] args) {
        final int NBOUTONS = 20;//nombre de boutons sur la calculatrice
        JButton[] boutons;//tableau de boutons
        JTextField txt;//champ de texte pour afficher les résultats et les entrées
        JPanel pan;//panel pour les boutons
        JFrame frame = new JFrame("Calculatrice");//création de la fenêtre principale
        frame.setSize(300, 400);//taille de la fenêtre
        Container contenu = frame.getContentPane();//contenu de la fenêtre
        contenu.setLayout(new BorderLayout(10, 20));//ajout d'un espacement entre les composants
        contenu.setBackground(new Color(42, 75, 124)); // couleur de fond de la fenêtre
        pan = new JPanel();//panel pour les boutons
        pan.setBackground(new Color(42, 75, 124));// couleur de fond du panel 
        txt = new JTextField();//champ de texte pour afficher les résultats et les entrées
        txt.setFont(new Font("Thaoma", Font.PLAIN, 20));//police du texte et taille
        txt.setHorizontalAlignment(JTextField.RIGHT);//alignement du texte à droite
        txt.setPreferredSize(new Dimension(0, 40));//taille du champ de texte
        JPanel txtWrapper = new JPanel(new BorderLayout()); //panel pour le champ de texte
        txtWrapper.setBorder(new EmptyBorder(10, 10, 0, 10)); //ajout d'une bordure au champ de texte
        txtWrapper.add(txt, BorderLayout.CENTER);//ajout du champ de texte au panel
        txtWrapper.setBackground(new Color(42, 75, 124)); // couleur de fond du panel
        contenu.add(txtWrapper, BorderLayout.NORTH);//ajout du panel au contenu de la fenêtre
        pan.setLayout(new GridLayout(5, 4, 5, 5));  //ajout d'une grille de 5 lignes et 4 colonnes pour les boutons
        boutons = new JButton[NBOUTONS];//initialisation du tableau de boutons
        String[] b = {"C","±","%","÷","7","8","9","x","4","5","6","-","1","2","3","+","0",".","DEL","="};//tableau de texte pour les boutons
        for (int i = 0; i < NBOUTONS; i++) {
            boutons[i] = new JButton(b[i]);//initialisation des boutons avec le texte du tableau
            boutons[i].setFont(new Font("Arial", Font.BOLD, 16));//police du texte et taille
            boutons[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));//ajout d'une bordure aux boutons
            boutons[i].setFocusPainted(true); //focus sur le bouton cliqué
            if (i >= 4 && i <= 6 || i >= 8 && i <= 10 || i >= 12 && i <= 14 || i == 16) {
                boutons[i].setBackground(new Color(255, 180, 76)); // couleur orange pour les boutons numériques
                boutons[i].setForeground(Color.BLACK);// couleur du texte en noir
            } else {
                
                boutons[i].setBackground(Color.LIGHT_GRAY);// couleur gris clair pour les autres boutons
                boutons[i].setForeground(Color.BLACK);// couleur du texte en noir
            }
            
            if (i == 19) { 
                boutons[i].setBackground(new Color(169, 169, 169)); // couleur gris pour le bouton "="
            }

            pan.add(boutons[i]);//ajout des boutons au panel
        }
        JPanel panWrapper = new JPanel(new BorderLayout());//panel pour le panel de boutons
        panWrapper.setBorder(new EmptyBorder(0, 10, 10, 10));//ajout d'une bordure au panel de boutons
        panWrapper.add(pan, BorderLayout.CENTER);//ajout du panel de boutons au panel wrapper
        panWrapper.setBackground(new Color(42, 75, 124));// couleur de fond du panel wrapper
        contenu.add(panWrapper, BorderLayout.CENTER);//ajout du panel wrapper au contenu de la fenêtre
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//fermeture de la fenêtre à la fermeture de l'application
        frame.setLocationRelativeTo(null);//centrer la fenêtre sur l'écran
        frame.setVisible(true);//afficher la fenêtre
        try {//Initialiser le socket et l'adresse du serveur
            socket = new DatagramSocket();// Créer un socket UDP
            serverAddress = InetAddress.getByName(server_ip);// Convertir l'adresse IP du serveur en InetAddress
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Socket init failed: " + e.getMessage());//afficher un message d'erreur si l'initialisation échoue
        }
        ActionListener listener1 = new ActionListener() {//écouteur d'événements pour les boutons
            @Override
            public void actionPerformed(ActionEvent e) {//méthode appelée lors d'un clic sur un bouton
                JButton source = (JButton) e.getSource();//obtenir le bouton cliqué
                String text = source.getText();//obtenir le texte du bouton
                if ("+x÷%-".contains(text)) {//si le texte du bouton est un opérateur
                    if (txt.getText().length() > 0) {//si le champ de texte n'est pas vide
                        txt.setText(txt.getText() + text);//ajouter l'opérateur au champ de texte
                    }
                } else if ("C".equals(text)) {//si le bouton cliqué est "C"
                    txt.setText("");//vider le champ de texte
                    txt.setForeground(Color.BLACK); // remettre la couleur du texte en noir
                } else if ("=".equals(text)) {//si le bouton cliqué est "="
                    String expression = txt.getText();//obtenir l'expression du champ de texte
                    txt.setEnabled(false);// désactiver le champ de texte pendant le traitement
                    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {//classe SwingWorker utitaire pour exécuter le traitement en arrière-plan
                        @Override
                        protected String doInBackground() throws Exception {//méthode exécutée en arrière-plan
                            try {
                                sendToServer(expression, serverAddress, server_port);//envoyer l'expression au serveur

                                return receiveFromServer();//recevoir la réponse du serveur
                            } catch (Exception ex) {
                                return "Erreur: " + ex.getMessage();//retourner un message d'erreur si une exception se produit
                                
                            }
                        }
                        @Override
                        protected void done() {//méthode appelée lorsque le traitement en arrière-plan est terminé
                            try {
                                String result = get();//obtenir le résultat du traitement
                                if (result.startsWith("Erreur:")) {//si le résultat commence par "Erreur:"
                                    txt.setText(result);//afficher le message d'erreur dans le champ de texte
                                    txt.setForeground(Color.RED);// changer la couleur du texte en rouge
                                } else {
                                    txt.setText(result);//afficher le résultat dans le champ de texte
                                    txt.setForeground(Color.BLACK);// remettre la couleur du texte en noir
                                }
                            } catch (Exception e) {
                                txt.setText("Erreur: " + e.getMessage());//afficher un message d'erreur si une exception se produit
                                txt.setForeground(Color.RED);// changer la couleur du texte en rouge
                            } finally {
                                txt.setEnabled(true);// réactiver le champ de texte
                            }
                        }
                    };
                    worker.execute();//exécuter le traitement en arrière-plan
                } else if (".".equals(text)) {//si le bouton cliqué est "." c'est pour ajouter un point décimal
                    if (txt.getText().length() > 0 && !txt.getText().endsWith(".")) {//si le champ de texte n'est pas vide et ne se termine pas par "."
                        String currentText = txt.getText();//obtenir le texte actuel du champ de texte
                        int lastOperatorIndex = Math.max(
                            Math.max(currentText.lastIndexOf('+'), currentText.lastIndexOf('-')),
                            Math.max(currentText.lastIndexOf('x'), currentText.lastIndexOf('÷'))
                        );//trouver le dernier opérateur dans le texte
                        String currentNumber = currentText.substring(lastOperatorIndex + 1);//obtenir le nombre actuel après le dernier opérateur
                        if (!currentNumber.contains(".")) {//si le nombre actuel ne contient pas de "."
                            txt.setText(currentText + text);//ajouter "." au texte actuel
                        }
                    }
                } else if ("±".equals(text)) {//si le bouton cliqué est "±" c'est pour changer le signe du nombre
                    String currentText = txt.getText();//obtenir le texte actuel du champ de texte
                    if (currentText.length() > 0) {//si le champ de texte n'est pas vide
                        if (currentText.startsWith("-")) {//si le texte actuel commence par "-"
                            txt.setText(currentText.substring(1));//supprimer le "-" du texte actuel
                        } else {//si le texte actuel ne commence pas par "-"
                            txt.setText("-" + currentText);//
                        }
                    }
                } else if ("DEL".equals(text)) {//si le bouton cliqué est "DEL" c'est pour supprimer le dernier caractère
                    String currentText = txt.getText();//obtenir le texte actuel du champ de texte
                    if (currentText.length() > 0) {//si le champ de texte n'est pas vide
                        txt.setText(currentText.substring(0, currentText.length() - 1));//supprimer le dernier caractère du texte actuel
                    }
                } else {
                    txt.setText(txt.getText() + text);//ajouter le texte du bouton au champ de texte
                }
            }
        };
        for (int i = 0; i < NBOUTONS; i++) {
            boutons[i].addActionListener(listener1);//ajouter l'écouteur d'événements à chaque bouton
        }
    }
   private static void sendToServer(String message, InetAddress address, int port) throws IOException {//méthode pour envoyer un message au serveur
    int checksum = calculateChecksum(message);// Calculer le checksum du message
    String messageWithChecksum = message + "|" + checksum;// Ajouter le checksum au message
    String sending=messageWithChecksum.trim();//trim pour enlever les espaces au debut et fin du message
    byte[] buf = sending.getBytes();// Convertir le message en tableau d'octets
    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);// Créer un paquet UDP avec le message, l'adresse et le port du serveur
    socket.send(packet); // Envoyer le paquet au serveur
} 
    private static int calculateChecksum(String message) {//méthode pour calculer le checksum du message
        final int WORD_SIZE = 16; //taille de mot de code 
        final int CHECKSUM_SIZE = 16; //taille de checksum de code
        StringBuilder bitRepresentation = new StringBuilder();
        for (char c : message.toCharArray()) {// Convertir chaque caractère en représentation binaire
           
            String binaryChar = Integer.toBinaryString(c);
           
            while (binaryChar.length() < 16) {// Compléter avec des zéros à gauche pour atteindre 16 bits
                binaryChar = "0" + binaryChar;//
            }
            bitRepresentation.append(binaryChar);// Ajouter la représentation binaire du caractère à la chaîne
        }
        while (bitRepresentation.length() % WORD_SIZE != 0) {
            bitRepresentation.append("0");// Compléter avec des zéros à droite pour atteindre un multiple de WORD_SIZE
        }
        int sum = 0;// Somme des mots de 16 bits
        for (int i = 0; i < bitRepresentation.length(); i += WORD_SIZE) {
            String word = bitRepresentation.substring(i, Math.min(i + WORD_SIZE, bitRepresentation.length()));// Extraire un mot de 16 bits
            int wordValue = Integer.parseInt(word, 2);// Convertir le mot binaire en entier
            sum += wordValue;// Ajouter la valeur du mot à la somme
        }
        int checksum = sum & ((1 << CHECKSUM_SIZE) - 1);// Garder seulement CHECKSUM_SIZE bits de la somme
        checksum = ~checksum & ((1 << CHECKSUM_SIZE) - 1); // Complément à un pour obtenir le checksum
        return checksum;// Retourner le checksum
    }
    private static String receiveFromServer() throws IOException {//méthode pour recevoir un message du serveur
        socket.setSoTimeout(2000);  // Définir un délai d'attente de 2 secondes pour la réception
        byte[] buffer = new byte[4096];// Taille du tampon pour recevoir le message
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);// Créer un paquet pour recevoir le message
        try {
            socket.receive(packet);// Recevoir le paquet du serveur
            String result = new String(packet.getData(), 0, packet.getLength());// Convertir le message reçu en chaîne de caractères
            System.out.println("Received from server: " + result);// Afficher le message reçu
            return result;// Retourner le message reçu
        } catch (SocketTimeoutException e) {
            System.out.println("Socket timeout occurred");// Afficher un message d'erreur si le délai d'attente est dépassé
            try {
                System.out.println("Attempting second receive...");// Afficher un message pour indiquer la deuxième tentative de réception
                socket.receive(packet);// Recevoir le paquet du serveur à nouveau
                String result = new String(packet.getData(), 0, packet.getLength());// Convertir le message reçu en chaîne de caractères
                System.out.println("Received on second attempt: " + result);// Afficher le message reçu
                return result;// Retourner le message reçu
            } catch (Exception ex) {
                System.out.println("Second receive attempt failed: " + ex.getMessage());// Afficher un message d'erreur si la deuxième tentative échoue
                return "Erreur: Serveur ne répond pas";// Retourner un message d'erreur si le serveur ne répond pas
            }
        } finally {   
            socket.setSoTimeout(0);// Réinitialiser le délai d'attente à 0 pour désactiver le délai d'attente
        }
    }
}