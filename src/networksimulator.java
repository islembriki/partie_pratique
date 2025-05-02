import java.net.*;
public class networksimulator {
    private static DatagramSocket clientSocket;//le socket qui recoit les paquets du client
    private static DatagramSocket serverSocket;//le socket qui envoie les paquets au serveur
    private static double errorRate = 0.1;// 10% chance d'erreur
    public static void main(String[] args) throws Exception {//methode principale
        clientSocket = new DatagramSocket(5678);//ecouter le port 5678 pour les paquets du client
        serverSocket = new DatagramSocket();//creation d'un socket pour le serveur
        InetAddress serverAddress = InetAddress.getByName("localhost");//adresse du serveur
        int serverPort = 1234;
        System.out.println("Network simulator running...");//afficher un message pour indiquer que le simulateur est en cours d'execution
        while (true) {// boucle infinie pour recevoir les paquets du client
            byte[] buffer = new byte[1024];//buffer pour stocker les paquets recus
            DatagramPacket clientPacket = new DatagramPacket(buffer, buffer.length);//creation d'un paquet pour recevoir les paquets du client
            clientSocket.receive(clientPacket);//recevoir le paquet du client
            String data = new String(clientPacket.getData(), 0, clientPacket.getLength());//extraire les donnees du paquet recu et les convertir en chaine de caracteres
            System.out.println("Received from client: " + data);//afficher les donnees recues du client(but de debug et log)
            if (Math.random() < errorRate) {//verifier si une erreur doit etre introduite
                //introduire une erreur dans les donnees recues
                //par exemple, on peut modifier un caractere aleatoire dans le message
                data = introduceError(data);//appel de la methode pour introduire une erreur
                // On peut aussi simuler la perte de paquet en ne faisant rien ici
                System.out.println("Introduced error, new data: " + data);
            }
            byte[] modifiedData = data.getBytes();//convertir les donnees modifiees en tableau d'octets qui sera envoye au serveur
            DatagramPacket serverPacket = new DatagramPacket( modifiedData, modifiedData.length, serverAddress, serverPort); serverSocket.send(serverPacket);//envoyer le paquet modifie au serveur
            buffer = new byte[1024];//creation d'un nouveau buffer pour recevoir la reponse du serveur
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);//creation d'un paquet pour recevoir la reponse du serveur
            //transferer le paquet du serveur au client
            //alors networksimulator joue le role d'un pont entre le client et le serveur
            DatagramPacket clientResponsePacket = new DatagramPacket(responsePacket.getData(), responsePacket.getLength(),clientPacket.getAddress(),clientPacket.getPort());//creation d'un paquet pour envoyer la reponse du serveur au client
            clientSocket.send(clientResponsePacket);//envoyer la reponse du serveur au client
        }
    }
    //methode pour introduire une erreur dans les donnees recues
   private static String introduceError(String data) {
    //supposons que les donnees soient sous la forme "message|checksum"
    //par exemple: "Hello World|1234567890"
    //on va introduire une erreur dans la partie message(dataload) et garder la partie checksum intacte car la chance d'erreur de checksum est presque nulle
    String[] parts = data.split("\\|");//on divise les donnees en deux parties: message et checksum
    if (parts.length > 1) {//verifier si la partie checksum existe
        String message = parts[0];  //la partie avant le '|'
        String checksum = parts[1]; // la partie apres le '|'
        // Introduire une erreur dans le message
        char[] messageChars = message.toCharArray();//convertir le message en tableau de caracteres pour pouvoir modifier un caractere aleatoire
        if (messageChars.length > 0) {//verifier si le message n'est pas vide
            int pos = (int)(Math.random() * messageChars.length);//choisir un index aleatoire dans le tableau de caracteres
            messageChars[pos] = (char)(messageChars[pos] + 1);  //modifier le caractere a cet index
            // On peut aussi choisir de supprimer un caractere ou d'en ajouter un, selon le type d'erreur que l'on veut simuler
        }
        // Recombiner le message et le checksum
        return new String(messageChars) + "|" + checksum;
    } else {
        // Si le format n'est pas valide, on retourne les donnees originales
        return data;
    }
}
}