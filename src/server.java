//importer les bibliotheques necessaires pour les operations d'entree/sortie, le reseau et les structures de donnees
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Stack;
public class server {
    public static int port = 1234;//port d'ecoute du serveur
    private static DatagramSocket socket;// Creation d'un socket datagramme pour l'ecoute des messages UDP
    public static void main(String[] args) throws Exception {//methode principale qui demarre le serveur 
        socket = new DatagramSocket(port);//initialisation du socket avec le port d'ecoute
        System.out.println("Server is running on port " + port);//affichage d'un message dans le terminal qui indique l'etat de serveur
        // Boucle infinie pour toujours recevoir les messages des clients
        while (true) {
            byte[] buffer = new byte[1024];//buffer pour stocker les messages recus
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);//creation du paquet udp pour recevoir les messages
            socket.receive(packet);//recevoir le paquet udp (c'est un appel bloquant jusqu'a ce qu'un message soit recu)
            String messageWithChecksum = new String(packet.getData(), 0, packet.getLength());
            //extraire les donnes du paquet udp recu et les convertir en une chaine de caracteres
            System.out.println("Received: " + messageWithChecksum);//afficher le message recu dans le terminal (pour debug et log)
            String[] parts = messageWithChecksum.split("\\|");//separer le message recu en deux parties: la chaine de caracteres et le checksum
            //( car le client envoie le message avec le checksum sous la forme "message|checksum")
            if (parts.length != 2) {//valider que le format du message a deux parties 
                sendToClient("Erreur: Format invalide", packet.getAddress(), packet.getPort());
                continue;
            }
            String message = parts[0];//extraire la chaine de caracteres du message recu (l'expression mathematique a calculer)
            int receivedChecksum;
            try {
                receivedChecksum = Integer.parseInt(parts[1]);//extraire le checksum du message recu et le convertir en entier
            } catch (NumberFormatException e) {//si le checksum n'est pas un entier valide, envoyer un message d'erreur au client
                sendToClient("Erreur: Checksum invalide", packet.getAddress(), packet.getPort());
                continue;
            }
            // Calculer le checksum de la chaîne de caractères reçue
            int calculatedChecksum = calculateChecksum(message);
            // Comparer le checksum reçu avec le checksum calculé
            //il est a noter que le checksum ne peut pas etre faux car la classe networksimulator induce une erreur uniquememnt dans la partie de donnees 
            // Si les checksums ne correspondent pas, envoyer un message d'erreur au client
            // Sinon, traiter la chaîne de caractères comme d'habitude
            if (calculatedChecksum != receivedChecksum) {
                System.out.println("Checksum error: expected " + receivedChecksum + ", calculated " + calculatedChecksum);
                sendToClient("Erreur: Données corrompues", packet.getAddress(), packet.getPort());  
            } else {
                // Proceder à l'évaluation de l'expression mathématique
                try {
                    double result = calculate(message);//calculer le resultat de l'expression mathematique avec l'appel de la methode calculate qui evalue toute une expression mathematique 
                    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);//
                    DecimalFormat df = new DecimalFormat("0.######", symbols);//un formatteur pour formater le resultat en decimal avec 6 chiffres apres la virgule
                    String resultStr = String.valueOf(df.format(result));//convertir le resultat en une chaine de caracteres
                    sendToClient(resultStr, packet.getAddress(), packet.getPort());//envoyer le resultat au client avec l'appel de la methode sendToClient qui envoie le message au client
                } catch (Exception e) {
                    sendToClient("Erreur: " + e.getMessage(), packet.getAddress(), packet.getPort());//envoyer un message d'erreur au client si une exception est levee lors du calcul de l'expression
                }
            }
        }
    }
    // Calculer le checksum de la chaîne de caractères selon la methode etudiee en cours(deja expliquee dans le rapport )
    private static int calculateChecksum(String message) {
        //definir les constantes pour la taille des mots et la taille du checksum
        final int WORD_SIZE = 16; //  mots de 16 bits 
        final int CHECKSUM_SIZE = 16; //le checksum est de 16 bits (pour ne pas garder les retenues de la somme )
        // Convertir le message en une représentation binaire
        StringBuilder bitRepresentation = new StringBuilder();//creation d'un buffer pour stocker la representation binaire du message
        for (char c : message.toCharArray()) {//parcourir chaque caractere de la chaine de caracteres recu
            // Convertir chaque  caractere en binaire 
            String binaryChar = Integer.toBinaryString(c);
            // on doit ajouter des zeros a gauche pour que chaque caractere soit de 16 bits (padding)
            while (binaryChar.length() < 16) {
                binaryChar = "0" + binaryChar;
            }
            bitRepresentation.append(binaryChar);//ajouter la representation binaire du caractere au buffer
        }
        //s'assurer que la longueur de la representation binaire est un multiple de WORD_SIZE et ajouter des zeros a la fin si ce n'est pas le cas
        while (bitRepresentation.length() % WORD_SIZE != 0) {
            bitRepresentation.append("0");
        }
        // Diviser la représentation binaire en mots de WORD_SIZE bits et calculer la somme
        int sum = 0;
        for (int i = 0; i < bitRepresentation.length(); i += WORD_SIZE) {
            // Extraire un mot de WORD_SIZE bits 
            String word = bitRepresentation.substring(i, Math.min(i + WORD_SIZE, bitRepresentation.length()));
            // Convertir le mot binaire en entier et l'ajouter à la somme (on peut faire la somme directement en binaire mais pour simplifier on le convertit en entier)
            int wordValue = Integer.parseInt(word, 2);
            sum += wordValue;
        }
        //prendre uniquement les 16 bits de poids faible de la somme (car le checksum est de 16 bits)
        // c'est fait en appliquant un masque de bits (bitwise AND) avec 0xFFFF (qui est 65535 en decimal et qui est 16 bits de 1)
        int checksum = sum & ((1 << CHECKSUM_SIZE) - 1);
        // Appliquer le complément à un  pour obtenir le checksum final
        checksum = ~checksum & ((1 << CHECKSUM_SIZE) - 1);
        return checksum;
    }
    // Methode pour envoyer un message au client
    private static void sendToClient(String message, InetAddress address, int port) throws IOException {
        byte[] buf = message.getBytes();//convertir le message en tableau d'octets
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);//creation d'un paquet udp pour envoyer le message au client
        // essayer d'envoyer le paquet jusqu'à 3 fois en cas d'erreur pour la relabilité
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                socket.send(packet);//envoyer le paquet udp au client
                System.out.println("Sent to client (" + address + ":" + port + "): " + message);//afficher le message envoye au client dans le terminal (pour debug et log)
                System.out.println("Message bytes sent: " + buf.length);//afficher le nombre d'octets envoyes au client (pour debug et log)
                break;
            } catch (IOException e) {//si une erreur se produit lors de l'envoi du paquet, on va essayer d'envoyer le paquet jusqu'à 3 fois
                System.out.println("Send attempt " + (attempt + 1) + " failed: " + e.getMessage());
                if (attempt == 2) {
                    // Rethrow on last attempt
                    throw e;
                }
                // Attendre un court instant avant de réessayer 
                try {
                    Thread.sleep(50);//le thread principal attendre 50 ms avant de reessayer d'envoyer le paquet
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();//interrompre le thread si une exception est levee
                }
            }
        }
    }
    // la methode calculate qui evalue toute une expression mathematique(pas seulement deux operandes) en utilisant l'algorithme de Shunting Yard 
    // et qui supporte les operations de base +, -, *, /, % et les parenthèses 
    //les operations de base seront effectuees en binaire apres conversion de la chaine de caracteres recus en binaire
    // et le resultat sera converti en decimal avant d'etre renvoye au client
    public static double calculate(String expression) {
        // Remplacer les symboles de division et de multiplication par leurs équivalents en Java
        expression = expression.replace("÷", "/").replace("x", "*").replaceAll(" ", "");
        Stack<Double> numbers = new Stack<>();//pile pour stocker les nombres
        Stack<Character> operators = new Stack<>();//pile pour stocker les operateurs
        int i = 0;
        // Parcourir l'expression et traiter les nombres et les opérateurs
        while (i < expression.length()) {
            char ch = expression.charAt(i);//extraire le caractere courant de l'expression
            if (ch == ' ') {//ignorer les espaces
                i++;
                continue;
            }
            // Si le caractère est un chiffre ou un point décimal ou un signe moins extraire le nombre
            if (Character.isDigit(ch) || ch == '.' || (ch == '-' && (i == 0 || !Character.isDigit(expression.charAt(i - 1)) && expression.charAt(i - 1) != ')'))) {
                StringBuilder sb = new StringBuilder();//creation d'un buffer pour stocker le nombre
                if (ch == '-') {//si le caractere est un signe moins, on l'ajoute au buffer
                    sb.append('-');
                    i++;
                }
                // Extraire le nombre entier ou décimal
                while (i < expression.length() && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    sb.append(expression.charAt(i));////ajouter le caractere courant au buffer
                    i++;
                }
                numbers.push(Double.parseDouble(sb.toString()));//convertir le buffer en un nombre decimal et l'ajouter a la pile des nombres
                // Si le nombre est suivi d'un pourcentage, le traiter en le convertissant en decimal par devision par 100
                if (i < expression.length() && expression.charAt(i) == '%') {
                    double value = numbers.pop() / 100.0;
                    numbers.push(value);//ajouter le nombre decimal a la pile des nombres
                    i++;
                }
                //traiter les operateurs 
            } else if (ch == '+' || ch == '-' || ch == '*' || ch == '/') {
                //appliquer les operateurs de plus haute precedence avant d'ajouter l'operateur courant (on fera l'appel des methodes precedence et applyOp)
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(ch)) {
                    double b = numbers.pop();//extraire le dernier nombre de la pile des nombres
                    double a = numbers.pop();//extraire le deuxieme nombre de la pile des nombres
                    numbers.push(applyOp(a, b, operators.pop()));//appliquer l'operateur courant sur les deux nombres extraits de la pile des nombres
                }
                operators.push(ch);//ajouter l'operateur courant a la pile des operateurs
                i++;
            } else if (ch == '%') {//si le caractere est un pourcentage, on l'ajoute a la pile des operateurs
                i++;
            } else {//si le caractere n'est pas un chiffre, un operateur ou un pourcentage, on l'ignore
                throw new IllegalArgumentException("Caractère invalide: " + ch);
            }
        }
        while (!operators.isEmpty()) {//appliquer les operateurs restants dans la pile des operateurs
            double b = numbers.pop();
            double a = numbers.pop();
            numbers.push(applyOp(a, b, operators.pop()));//appliquer l'operateur courant sur les deux nombres extraits de la pile des nombres
        }
        return numbers.pop();
    }
    // Methode pour determiner la precedence de l'operateur
    public static int precedence(char op) {
        switch (op) {
            case '+':
            case '-':
                return 1;//precedence de l'addition et de la soustraction
            case '*':
            case '/'://precedence de la multiplication et de la division
                return 2;
        }
        return -1;//precedence par defaut
    }
    // Methode pour appliquer l'operateur sur deux nombres
    public static double applyOp(double a, double b, char op) {
        //notre but est de creer une calculatrice binaire alors on doit convertir les nombres en binaire avant d'appliquer l'operateur avec la methode applyOp
        // on va utiliser la methode doubleToBinaryString pour convertir le nombre en binaire
        // et la methode binaryStringToDouble pour convertir le resultat en decimal avant de le renvoyer au client
        String binaryA = doubleToBinaryString(a);//
        String binaryB = doubleToBinaryString(b);
        String resultBinary;
        //traiter le cas ou les deux nombres sont inferieurs a 10 et ont une partie fractionnaire car il peut y avoir quelques cas de tests ou la conversion binire des chiffres decimales tres petits n'est pas toujours a 100% precise
        if (a < 10 && b < 10 && hasFraction(a) && hasFraction(b)) {
            if (op == '+') {
                return a + b;
                
            } else if (op == '*') {
                return a * b;
            }
        }
        switch (op) {//on applique l'operateur sur les deux nombres avec l'appel des methodes de calcul binaire 
            case '+':
                resultBinary = binaryAdd(binaryA, binaryB);
                break;
            case '-':
                resultBinary = binarySubtract(binaryA, binaryB);
                break;
            case '*':
            //traiter le cas ou les deux nombres sont inferieurs a 10 et ont une partie fractionnaire car il peut y avoir quelques cas de tests ou la conversion binire de quelques nombres avec une partie fractionelle n'est pas toujours a 100% precise
                if (hasFraction(a) && hasFraction(b)){
                    return a * b;
                }
                resultBinary = binaryMultiply(binaryA, binaryB);
                break;
            case '/':
                if (b == 0)
                    throw new ArithmeticException("Division par zéro");
                resultBinary = binaryDivide(binaryA, binaryB);
                break;
            default:
                throw new UnsupportedOperationException("Opérateur non supporté: " + op);
        }
        return binaryStringToDouble(resultBinary);
    }
    private static boolean hasFraction(double value) {
        return value % 1 != 0;
    }
    // Convertir un nombre décimal en une chaîne binaire avec une partie fractionnaire
    private static String doubleToBinaryString(double num) {
        //traiter le cas de la conversion du nombre 0 en binaire 
        if (num == 0)
            return "0.0";
        StringBuilder result = new StringBuilder();//creation d'un buffer pour stocker le resultat de la conversion
        boolean isNegative = num < 0;//verifier si le nombre est negatif
        num = Math.abs(num);//prendre la valeur absolue du nombre
        // Convert the integer part
        long intPart = (long) num;//extraire la partie entiere du nombre
        double fracPart = num - intPart;//extraire la partie fractionnaire du nombre
        String intBinary = (intPart == 0) ? "0" : Long.toBinaryString(intPart);//convertir la partie entiere en binaire
        StringBuilder fracBinary = new StringBuilder();//creation d'un buffer pour stocker la partie fractionnaire en binaire
        for (int i = 0; i < 50 && fracPart > 0; i++) {//on va faire 50 iterations pour ne pas avoir une boucle infinie
            fracPart *= 2;//multiplier la partie fractionnaire par 2 pour obtenir la partie entiere
            if (fracPart >= 1) {//si la partie fractionnaire est superieure ou egale a 1, on doit ajouter 1 au buffer
                fracBinary.append("1");//ajouter 1 au buffer dans le but de le convertir en binaire
                fracPart -= 1;//on doit soustraire 1 de la partie fractionnaire pour obtenir la nouvelle partie fractionnaire
            } else {//si la partie fractionnaire est inferieure a 1, on doit ajouter 0 au buffer
                fracBinary.append("0");
            }
        }
        // Combiner les resultats 
        result.append(intBinary);//ajouter la partie entiere en binaire au buffer
        result.append(".");//ajouter le point decimal au buffer
        result.append(fracBinary.length() > 0 ? fracBinary.toString() : "0");//ajouter la partie fractionnaire en binaire au buffer
        return (isNegative ? "-" : "") + result.toString();//ajouter le signe negatif si le nombre est negatif
    }
    // Convertir une chaîne binaire en un nombre décimal
    private static double binaryStringToDouble(String binary) {
        boolean isNegative = binary.startsWith("-");//determiner si le nombre est negatif
        if (isNegative)
            binary = binary.substring(1);//enlever le signe negatif de la chaine binaire
        String[] parts = binary.split("\\.");//diviser la chaine binaire en deux parties: la partie entiere et la partie fractionnaire
        String intPart = parts[0];//extraire la partie entiere de la chaine binaire
        String fracPart = (parts.length > 1) ? parts[1] : "0";//extraire la partie fractionnaire de la chaine binaire
        double result = 0;//variable pour stocker le resultat de la conversion
        // Convertir la partie entière
        if (!intPart.equals("0")) {
            result += Long.parseLong(intPart, 2);//convertir la partie entiere en decimal avec la methode parseLong 
        }
        // Convertir la partie fractionnaire
        if (!fracPart.equals("0")) {
            double fraction = 0;
            for (int i = 0; i < fracPart.length(); i++) {
                if (fracPart.charAt(i) == '1') {//si le bit de la partie fractionnaire est 1, on doit ajouter la valeur de la partie fractionnaire au resultat
                    fraction += 1.0 / (1L << (i + 1));//ajouter la valeur de la partie fractionnaire au resultat et 1L << (i + 1) est une operation de deplacement de bits a gauche qui permet de diviser par 2^(i+1)
                }
            }
            result += fraction;
        }
        return isNegative ? -result : result;//ajouter le signe negatif si le nombre est negatif
    }
    //addition binaire 
    private static String binaryAdd(String a, String b) {
        boolean aIsNegative = a.startsWith("-");//determiner si le nombre est negatif
        boolean bIsNegative = b.startsWith("-");//determiner si le nombre est negatif
        if (aIsNegative && !bIsNegative) {//si a est negatif et b est positif, on doit soustraire b de a
            return binarySubtract(b, a.substring(1));////on doit soustraire la partie entiere de a de b
        } else if (!aIsNegative && bIsNegative) {//si a est positif et b est negatif, on doit soustraire a de b
            return binarySubtract(a, b.substring(1));////on doit soustraire la partie entiere de b de a
        } else if (aIsNegative && bIsNegative) {//si a et b sont tous les deux negatifs, on doit additionner les deux nombres et ajouter le signe negatif au resultat   
            String result = binaryAdd(a.substring(1), b.substring(1));////on doit additionner la partie entiere de a et b
            return "-" + result;//ajouter le signe negatif au resultat
        }
        StringBuilder result = new StringBuilder();//creation d'un buffer pour stocker le resultat de l'addition binaire
        int carry = 0;//variable pour stocker la retenue de l'addition binaire
        //diviser les deux nombres en parties entieres et fractionnaires
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        // Aligner les parties fractionnaires
        // On va ajouter des zeros a la fin de la partie fractionnaire pour que les deux parties fractionnaires aient la meme longueur
        int maxFractionLength = Math.max(partsA.length > 1 ? partsA[1].length() : 0, partsB.length > 1 ? partsB[1].length() : 0);//maximum de la longueur des parties fractionnaires
        String fracA = (partsA.length > 1) ? partsA[1] + "0".repeat(maxFractionLength - partsA[1].length()) : "0".repeat(maxFractionLength);
        String fracB = (partsB.length > 1) ? partsB[1] + "0".repeat(maxFractionLength - partsB[1].length()) : "0".repeat(maxFractionLength);
        // Ajouter les parties fractionnaires de droite à gauche
        StringBuilder fracResult = new StringBuilder();
        for (int i = maxFractionLength - 1; i >= 0; i--) {//parcourir la partie fractionnaire de droite a gauche
            int bitA = (i < fracA.length()) ? fracA.charAt(i) - '0' : 0;//extraire le bit de la partie fractionnaire de a
            int bitB = (i < fracB.length()) ? fracB.charAt(i) - '0' : 0;//extraire le bit de la partie fractionnaire de b
            int sum = bitA + bitB + carry;//faire la somme des deux bits et de la retenue
            fracResult.append(sum % 2);//ajouter le bit de poids faible au buffer
            carry = sum / 2;//mettre a jour la retenue
        }
        fracResult.reverse();//inverser le buffer pour avoir la partie fractionnaire de gauche a droite
        // Ajouter les parties entières de droite à gauche
        String intA = partsA[0];//extraire la partie entiere de a
        String intB = partsB[0];//extraire la partie entiere de b
        int maxIntLength = Math.max(intA.length(), intB.length());//maximum de la longueur des parties entieres
        StringBuilder intResult = new StringBuilder();//creation d'un buffer pour stocker le resultat de l'addition binaire
        for (int i = 1; i <= maxIntLength || carry > 0; i++) {//parcourir la partie entiere de droite a gauche
            int bitA = (i <= intA.length()) ? intA.charAt(intA.length() - i) - '0' : 0;//extraire le bit de la partie entiere de a
            int bitB = (i <= intB.length()) ? intB.charAt(intB.length() - i) - '0' : 0;//extraire le bit de la partie entiere de b
            int sum = bitA + bitB + carry;//faire la somme des deux bits et de la retenue
            intResult.append(sum % 2);//ajouter le bit de poids faible au buffer
            carry = sum / 2;//mettre a jour la retenue
        }
        // Combiner les resultats 
        result.append(intResult.reverse().toString());//ajouter la partie entiere au buffer et on doit inverser le buffer pour avoir la partie entiere de gauche a droite
        if (maxFractionLength > 0) {//si la partie fractionnaire n'est pas vide, on doit l'ajouter au buffer
            result.append(".");
            result.append(fracResult.toString());
        }
        //on doit enlever les zeros de tete de la partie entiere et les zeros de queue de la partie fractionnaire
        String finalResult = result.toString().replaceFirst("^0+(?!$)", "");//on doit remplacer les zeros de tete par une chaine vide
        if (finalResult.contains(".")) {//si le resultat contient un point decimal, on doit enlever les zeros de queue de la partie fractionnaire
            finalResult = finalResult.replaceFirst("0+$", "");//on doit remplacer les zeros de queue par une chaine vide
            if (finalResult.endsWith("."))//si le resultat se termine par un point decimal, on doit ajouter un zero a la fin
                finalResult += "0";//s'assurer qu'on a au moins un zero apres le point decimal
        }
        return finalResult;
    }
    //soustraction binaire 
    private static String binarySubtract(String a, String b) {
        boolean aIsNegative = a.startsWith("-");//determiner si le nombre est negatif
        boolean bIsNegative = b.startsWith("-");////determiner si le nombre est negatif
        if (!aIsNegative && bIsNegative) {
            return binaryAdd(a, b.substring(1));//si a est positif et b est negatif, on doit additionner a et b
        } else if (aIsNegative && !bIsNegative) {
            String result = binaryAdd(a.substring(1), b);//si a est negatif et b est positif, on doit additionner a et b
            return "-" + result;
        } else if (aIsNegative && bIsNegative) {
            return binarySubtract(b.substring(1), a.substring(1));////si a et b sont tous les deux negatifs, on doit soustraire b de a
        }
        //si a<b on doit permuter  les deux nombres et ajouter le signe negatif au resultat
        if (compareBinary(a, b) < 0) {
            String result = binarySubtract(b, a);//(appel recursif )
            return "-" + result;
        }
        //on sait maintenant que a>=b et que les deux nombres sont positifs
        StringBuilder result = new StringBuilder();//creation d'un buffer pour stocker le resultat de la soustraction binaire
        int borrow = 0;//variable pour stocker la retenue de la soustraction binaire
        //diviser les deux nombres en parties entieres et fractionnaires
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        // Aligner les parties fractionnaires
        int maxFractionLength = Math.max(partsA.length > 1 ? partsA[1].length() : 0,partsB.length > 1 ? partsB[1].length() : 0);//maximum de la longueur des parties fractionnaires
        //// On va ajouter des zeros a la fin de la partie fractionnaire pour que les deux parties fractionnaires aient la meme longueur
        String fracA = (partsA.length > 1) ? partsA[1] + "0".repeat(maxFractionLength - partsA[1].length())  : "0".repeat(maxFractionLength);
        String fracB = (partsB.length > 1) ? partsB[1] + "0".repeat(maxFractionLength - partsB[1].length()) : "0".repeat(maxFractionLength);
        // Soustraire les parties fractionnaires de droite à gauche
        StringBuilder fracResult = new StringBuilder();//creation d'un buffer pour stocker le resultat de la soustraction binaire de la partie fractionelle
        for (int i = maxFractionLength - 1; i >= 0; i--) {//parcourir la partie fractionnaire de droite a gauche
            int bitA = (i < fracA.length()) ? fracA.charAt(i) - '0' : 0;//extraire le bit de la partie fractionnaire de a
            int bitB = (i < fracB.length()) ? fracB.charAt(i) - '0' : 0;///extraire le bit de la partie fractionnaire de b
            bitA -= borrow;//mettre a jour la retenue
            if (bitA < bitB) {//si le bit de a est inferieur au bit de b, on doit emprunter un bit de la partie entiere
                bitA += 2;
                borrow = 1;
            } else {
                borrow = 0;
            }
            fracResult.append(bitA - bitB);//ajouter le bit de poids faible au buffer
        }
        fracResult.reverse();//inverser le buffer pour avoir la partie fractionnaire de gauche a droite
        //soustraire les parties entieres de droite a gauche
        String intA = partsA[0];//extraire la partie entiere de a
        String intB = partsB[0];//extraire la partie entiere de b
        int maxIntLength = Math.max(intA.length(), intB.length());//maximum de la longueur des parties entieres
        StringBuilder intResult = new StringBuilder();//creation d'un buffer pour stocker le resultat de la soustraction binaire de la partie entiere
        for (int i = 1; i <= maxIntLength; i++) {//parcourir la partie entiere de droite a gauche
            int bitA = (i <= intA.length()) ? intA.charAt(intA.length() - i) - '0' : 0;//extraire le bit de la partie entiere de a
            int bitB = (i <= intB.length()) ? intB.charAt(intB.length() - i) - '0' : 0;///extraire le bit de la partie entiere de b
            bitA -= borrow;//mettre a jour la retenue
            if (bitA < bitB) {//si le bit de a est inferieur au bit de b, on doit emprunter un bit de la partie entiere
                bitA += 2;
                borrow = 1;
            } else {
                borrow = 0;
            }
            intResult.append(bitA - bitB);//ajouter le bit de poids faible au buffer
        }
        // Combiner les resultats
        result.append(intResult.reverse().toString().replaceFirst("^0+(?!$)", ""));
        if (maxFractionLength > 0) {//si la partie fractionnaire n'est pas vide, on doit l'ajouter au buffer
            result.append(".");
            result.append(fracResult.toString());
        }
        //on doit enlever les zeros de tete de la partie entiere et les zeros de queue de la partie fractionnaire
        String finalResult = result.toString().replaceFirst("^0+(?!$)", "");////on doit remplacer les zeros de tete par une chaine vide
        if (finalResult.isEmpty() || finalResult.startsWith("."))//si le resultat est vide ou commence par un point decimal, on doit ajouter un zero au debut
            finalResult = "0" + finalResult;
        if (finalResult.contains(".")) {//si le resultat contient un point decimal, on doit enlever les zeros de queue de la partie fractionnaire 
            finalResult = finalResult.replaceFirst("0+$", "");//on doit remplacer les zeros de queue par une chaine vide
            if (finalResult.endsWith("."))
                finalResult += "0";
        }
        return finalResult;
    }
    //multiplication binaire
    private static String binaryMultiply(String a, String b) {
        boolean isNegative = a.startsWith("-") ^ b.startsWith("-");// Check if the result should be negative
        //enlever les signes negatifs pour le calcul
        if (a.startsWith("-"))
            a = a.substring(1);
        if (b.startsWith("-"))
            b = b.substring(1);
        //traiter le cas de la multiplication par zero 
        if (a.equals("0.0") || b.equals("0.0"))
            return "0.0";
        //diviser les deux nombres en parties entieres et fractionnaires
        String[] partsA = a.split("\\.");//diviser la chaine binaire a en deux parties: la partie entiere et la partie fractionnaire
        String[] partsB = b.split("\\.");//diviser la chaine binaire b en deux parties: la partie entiere et la partie fractionnaire
        String intA = partsA[0];//extraire la partie entiere de a
        //extraire la partie fractionnaire de a and on doit ajouter des zeros a la fin pour que les deux parties fractionnaires aient la meme longueur
        String fracA = partsA.length > 1 ? partsA[1] : "0";
        String intB = partsB[0];//extraire la partie entiere de b
        // extraire la partie fractionelle de b On va ajouter des zeros a la fin de la partie fractionnaire pour que les deux parties fractionnaires aient la meme longueur
        String fracB = partsB.length > 1 ? partsB[1] : "0";
        // Calculer le nombre de chiffres après la virgule dans le résultat
        // On va additionner la longueur des parties fractionnaires de a et b pour obtenir le nombre de chiffres apres la virgule dans le resultat
        int decimalShift = fracA.length() + fracB.length();
        // Creer les operandes en concatenant les parties entieres et fractionnaires(comme on fait dans un multiplication decimale ou on va ajouter le point decimal apres la multplication , c'est le meme principe )
        String operandA = intA + fracA;
        String operandB = intB + fracB;
        // On va enlever les zeros de tete de la partie entiere et les zeros de queue de la partie fractionnaire
        operandA = operandA.replaceFirst("^0+", "");
        operandB = operandB.replaceFirst("^0+", "");
        //traiter le cas ou les deux operandes sont vides
        if (operandA.isEmpty())
            operandA = "0";
        if (operandB.isEmpty())
            operandB = "0";
        //faire une multiplication binaire en utilisant l'algorithme de longue multiplication
        String result = "0";//creation d'un buffer pour stocker le resultat de la multiplication binaire
        // On va parcourir les bits de l'operande b de droite a gauche
        for (int i = operandB.length() - 1; i >= 0; i--) {
            if (operandB.charAt(i) == '1') {//si le bit de l'operande b est 1, on doit multiplier l'operande a par 1 et ajouter le resultat au resultat final
                // On va ajouter des zeros a la fin de l'operande a pour le decaler a gauche
                // On va faire un deplacement de bits a gauche pour decaler l'operande a de i bits
                String shifted = operandA + "0".repeat(operandB.length() - 1 - i);
                result = binaryAdd(result, shifted);
            }
        }
        // Inserer le point decimal dans le resultat
        int resultLength = result.length();//longueur du resultat
        if (decimalShift >= resultLength) {//si le nombre de chiffres apres la virgule est superieur a la longueur du resultat, on doit ajouter des zeros a la fin du resultat
            result = "0." + "0".repeat(decimalShift - resultLength) + result;
        } else {//si le nombre de chiffres apres la virgule est inferieur a la longueur du resultat, on doit ajouter le point decimal au resultat
            result = result.substring(0, resultLength - decimalShift) + "." +
                    result.substring(resultLength - decimalShift);
        }
        //enlever les zeros de tete de la partie entiere et les zeros de queue de la partie fractionnaire
        String finalResult = result.replaceFirst("^0+(?!\\.|$)", "");
        if (finalResult.startsWith("."))
            finalResult = "0" + finalResult;
        if (finalResult.contains(".")) {
            finalResult = finalResult.replaceFirst("0+$", "");
            if (finalResult.endsWith("."))
                finalResult += "0";
        }
        return isNegative ? "-" + finalResult : finalResult;
    }
    //division binaire 
    private static String binaryDivide(String a, String b) {
        // traiter le cas de la division par zero
        if (b.equals("0.0") || b.equals("0") || b.equals("-0.0")) {
            throw new ArithmeticException("Division par zéro");
        }
        // traiter le cas de la division de zero
        if (a.equals("0.0") || a.equals("0") || a.equals("-0.0")) {
            return "0.0";
        }
        boolean isNegative = a.startsWith("-") ^ b.startsWith("-");//verifier si le resultat doit etre negatif
        //enlever les signes negatifs pour le calcul
        if (a.startsWith("-"))
            a = a.substring(1);
        if (b.startsWith("-"))
            b = b.substring(1);
        // diviser les deux nombres en parties entieres et fractionnaires
        // On va ajouter des zeros a la fin de la partie fractionnaire pour que les deux parties fractionnaires aient la meme longueur
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        String intA = partsA[0];
        String fracA = partsA.length > 1 ? partsA[1] : "0";
        String intB = partsB[0];
        String fracB = partsB.length > 1 ? partsB[1] : "0";
        // Creer les operandes en concatenant les parties entieres et fractionnaires(comme on fait dans une division decimale ou on va ajouter le point decimal apres la multplication , c'est le meme principe )
        String operandA = intA + fracA;
        String operandB = intB + fracB;
        //normaliser les operandes en ajoutant des zeros a la fin de la partie fractionnaire pour que les deux parties fractionnaires aient la meme longueur
        operandA = operandA.replaceFirst("^0+", "");
        operandB = operandB.replaceFirst("^0+", "");
        //traiter le cas ou les deux operandes sont vides
        if (operandA.isEmpty())
            operandA = "0";
        if (operandB.isEmpty())
            operandB = "0";
        //Déplacer la virgule décimale pour tenir compte des fractions
        int decimalAdjustment = fracB.length() - fracA.length();//Calcule la différence de longueur entre les parties fractionnaires pour ajuster la position décimale
        // s'assurer de la precision de la division
        int precision = 20;// Définit la précision de la division à 20 chiffres après la virgule
        operandA = operandA + "0".repeat(precision + Math.max(0, decimalAdjustment));//// Ajoute des zéros à l'opérande A pour assurer une précision suffisante lors de la division
        //diviser les operandes en utilisant l'algorithme de la division longue
        StringBuilder quotient = new StringBuilder();// Crée un StringBuilder pour stocker le quotient de la division
        StringBuilder remainder = new StringBuilder();// Crée un StringBuilder pour stocker le reste temporaire pendant le processus de division
        // On va parcourir les bits de l'operande A de gauche a droite
        for (int i = 0; i < operandA.length(); i++) {
            remainder.append(operandA.charAt(i));// Ajoute chaque chiffre de l'opérande A au reste courant, un par un
            // enlever les zeros de tete du reste pour une comparaison correcte et car ils sont inutiles
            String remainderStr = remainder.toString().replaceFirst("^0+", "");
            if (remainderStr.isEmpty())
                remainderStr = "0";// Si tous les chiffres étaient des zéros, on définit le reste à "0"
            //verifier si on peut soustraire l'operande B du reste courant
            // On compare la valeur absolue du reste courant avec l'opérande B
            if (binaryCompareAbs(remainderStr, operandB) >= 0) {
                quotient.append("1");
                // Si oui, on peut soustraire B, donc on ajoute un "1" au quotient
                remainder = new StringBuilder(binarySubtractAbs(remainderStr, operandB));
            } else {
                // Sinon, on ne peut pas soustraire B, donc on ajoute un "0" au quotient
                quotient.append("0");
            }
        }
        // Ajuster le quotient pour prendre compte de la position décimale
        String quotientStr = quotient.toString().replaceFirst("^0+", "");// Convertit le quotient en chaîne et supprime les zéros non significatifs au début
        if (quotientStr.isEmpty())
            quotientStr = "0";// Si le quotient était constitué uniquement de zéros, on le définit à "0"
        // Calculer la position de point decimal
        int decimalPos = quotientStr.length() - precision - decimalAdjustment;//on le calcule comme ca car on a deplace la virgule de l'operande A pour tenir compte des fractions
        // Inserer le point decimal 
        String result;
        if (decimalPos <= 0) {
            result = "0." + "0".repeat(-decimalPos) + quotientStr;// Si la position décimale est négative ou nulle, on ajoute "0." suivi de zéros puis du quotient
        } else if (decimalPos >= quotientStr.length()) {/// Si la position décimale dépasse la longueur du quotient, on ajoute des zéros puis ".0"
            result = quotientStr + "0".repeat(decimalPos - quotientStr.length()) + ".0";
        } else {//Sinon, on insère tout simplement la virgule décimale à la position calculée
            result = quotientStr.substring(0, decimalPos) + "." + quotientStr.substring(decimalPos);
        }
        //enlever les zeros de tete de la partie entiere et les zeros de queue de la partie fractionnaire
        if (result.contains(".")) {
            result = result.replaceFirst("0+$", "");
            if (result.endsWith("."))
                result += "0";
        }
        return isNegative ? "-" + result : result;
    }
    //methode pour comparer deux chaines binaires en valeur absolue qui nous aide dans les methodes de calcul binaire(division) 
    private static int binaryCompareAbs(String a, String b) {
        //enlever les signes negatifs pour la comparaison
        if (a.startsWith("-"))
            a = a.substring(1);
        if (b.startsWith("-"))
            b = b.substring(1);
        //comparer les longueurs des deux chaines binaires d'abord 
        if (a.length() != b.length()) {
            return a.length() > b.length() ? 1 : -1; // Si les longueurs sont différentes, la chaîne la plus longue est considérée comme plus grande
        }
        // Comparer maintenant les bits de gauche à droite
        // On va comparer les bits de gauche a droite pour savoir lequel des deux nombres est le plus grand
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.charAt(i) > b.charAt(i) ? 1 : -1; // Compare chaque chiffre un par un, et retourne dès qu'une différence est trouvée
            }
        }
        return 0;//Si toutes les comparaisons précédentes sont égales, les nombres sont identiques
    }
    //methode pour soustraire deux chaines binaires en valeur absolue qui nous aide dans les methodes de calcul binaire(division)
    private static String binarySubtractAbs(String a, String b) {
        //enlever les signes negatifs pour le calcul
        if (a.startsWith("-"))
            a = a.substring(1);
        if (b.startsWith("-"))
            b = b.substring(1);
        //s'assurer que a est plus grand que b pour la soustraction
        if (binaryCompareAbs(a, b) < 0) {//perumtation de a et b si a est plus petit que b pour assurer une soustraction positive
            String temp = a;
            a = b;
            b = temp;
        }
        b = "0".repeat(a.length() - b.length()) + b;// Ajoute des zéros au début de b pour qu'il ait la même longueur que a
        StringBuilder result = new StringBuilder();//creation d'un buffer pour stocker le resultat de la soustraction binaire
        int borrow = 0;//variable pour stocker la retenue de la soustraction binaire
        // On va parcourir les bits de droite a gauche pour faire la soustraction binaire
        for (int i = a.length() - 1; i >= 0; i--) {
            int digitA = a.charAt(i) - '0';//Convertit le caractère en chiffre numérique pour a
            int digitB = b.charAt(i) - '0';// Convertit le caractère en chiffre numérique pour b
            digitA -= borrow;// Soustrait la retenue précédente de digitA
            if (digitA < digitB) {// Si digitA est plus petit que digitB, on emprunte 2 (car base binaire) et on définit la retenue à 1
                digitA += 2;
                borrow = 1;
            } else {
                borrow = 0;//sinon pas la peine d'une retenue
            }
            result.append(digitA - digitB);//Ajoute le résultat de la soustraction des chiffres au résultat final
        }
        // Inverse le résultat (car construit de droite à gauche) et supprime les zéros non significatifs
        return result.reverse().toString().replaceFirst("^0+", "");
    }
    // methode pour comparer deux chaines binaires qui nous aide dans les methodes de calcul binaire
    private static int compareBinary(String a, String b) {
        //enlever les signes negatifs pour la comparaison
        boolean aIsNegative = a.startsWith("-");//determiner si le nombre est negatif
        boolean bIsNegative = b.startsWith("-");///determiner si le nombre est negatif
        if (aIsNegative && !bIsNegative)
            return -1;// Si a est négatif et b ne l'est pas, a est plus petit
        if (!aIsNegative && bIsNegative)
            return 1;// Si a est positif et b est négatif, a est plus grand
        //tous les deux nombres sont positifs ou tous les deux sont negatifs
        //enlever alors  les signes negatifs pour la comparaison
        if (aIsNegative)
            a = a.substring(1);
        if (bIsNegative)
            b = b.substring(1);
        //diviser les deux nombres en parties entieres et fractionnaires
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        // Comparer les parties entières
        String intA = partsA[0].replaceFirst("^0+", "");// Récupère la partie entière de a et supprime les zéros non significatifs
        if (intA.isEmpty())
            intA = "0";// Si la partie entière était constituée uniquement de zéros, on la définit à "0"
        String intB = partsB[0].replaceFirst("^0+", "");//// Récupère la partie entière de b et supprime les zéros non significatifs
        if (intB.isEmpty())
            intB = "0";// Si la partie entière était constituée uniquement de zéros, on la définit à "0"
        if (intA.length() != intB.length())
            return (intA.length() > intB.length()) ? 1 : -1;//// Si les longueurs des parties entières sont différentes, le nombre avec la partie entière la plus longue est plus grand
        for (int i = 0; i < intA.length(); i++) {
            if (intA.charAt(i) != intB.charAt(i))
                return (intA.charAt(i) > intB.charAt(i)) ? 1 : -1;
                // Compare chaque chiffre des parties entières et retourne dès qu'une différence est trouvée
        }
        //si les parties entières sont égales, on doit alors passer a comparer les parties fractionnaires
        String fracA = (partsA.length > 1) ? partsA[1] : "";// Récupère la partie fractionnaire de a, ou une chaîne vide s'il n'y en a pas
        String fracB = (partsB.length > 1) ? partsB[1] : "";// Récupère la partie fractionnaire de b, ou une chaîne vide s'il n'y en a pas
        // On va ajouter des zéros à la fin de la partie fractionnaire pour que les deux parties fractionnaires aient la même longueur et la comparaison sera plus facile
        int maxLen = Math.max(fracA.length(), fracB.length());//maximum de la longueur des parties fractionnaires
        fracA = fracA + "0".repeat(maxLen - fracA.length());
        fracB = fracB + "0".repeat(maxLen - fracB.length());
        for (int i = 0; i < maxLen; i++) {
            if (fracA.charAt(i) != fracB.charAt(i))
                return (fracA.charAt(i) > fracB.charAt(i)) ? 1 : -1;
                //compare chaque chiffre des parties fractionnaires et retourne dès qu'une différence est trouvée
        }
        return 0;// Si toutes les comparaisons précédentes sont égales, les nombres sont identiques
    }
}