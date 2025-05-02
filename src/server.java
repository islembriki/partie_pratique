//importer les bibliotheques necessaires pour l'implementation des datagrams sockets(UDP)
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
                    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                    DecimalFormat df = new DecimalFormat("0.######", symbols);
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
    // la methode calculate qui evalue toute une expression mathematique en utilisant l'algorithme de Shunting Yard 
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
                    double b = numbers.pop();
                    double a = numbers.pop();
                    numbers.push(applyOp(a, b, operators.pop()));
                }
                operators.push(ch);
                i++;
            } else if (ch == '%') {
                i++;
            } else {
                throw new IllegalArgumentException("Caractère invalide: " + ch);
            }
        }
        while (!operators.isEmpty()) {
            double b = numbers.pop();
            double a = numbers.pop();
            numbers.push(applyOp(a, b, operators.pop()));
        }
        return numbers.pop();
    }
    public static int precedence(char op) {
        switch (op) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
                return 2;
        }
        return -1;
    }
    public static double applyOp(double a, double b, char op) {
        // Convert numbers to their binary string representations with decimal points
        String binaryA = doubleToBinaryString(a);
        String binaryB = doubleToBinaryString(b);
        String resultBinary;
        switch (op) {
            case '+':
                resultBinary = binaryAdd(binaryA, binaryB);
                break;
            case '-':
                resultBinary = binarySubtract(binaryA, binaryB);
                break;
            case '*':
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
    // Convert a double to a binary string with decimal point//khtr adition ta
    // azouza tlwj taaml kisma fi wist bi . w ken dima bi norme iee moch mnjm
    // yalkaha
    /*
     * The error you were seeing "Index 1 out of bounds for length 1" was
     * because your friend's code expected strings with decimal points,
     * but your code was passing IEEE 754 binary representations without decimal
     * points.
     */
    private static String doubleToBinaryString(double num) {
        if (num == 0)
            return "0.0";
        StringBuilder result = new StringBuilder();
        boolean isNegative = num < 0;
        num = Math.abs(num);
        // Convert the integer part
        long intPart = (long) num;
        double fracPart = num - intPart;
        // Convert integer part to binary
        String intBinary = (intPart == 0) ? "0" : Long.toBinaryString(intPart);
        // Convert fractional part to binary (limited precision)
        StringBuilder fracBinary = new StringBuilder();
        for (int i = 0; i < 50 && fracPart > 0; i++) {
            fracPart *= 2;
            if (fracPart >= 1) {
                fracBinary.append("1");
                fracPart -= 1;
            } else {
                fracBinary.append("0");
            }
        }
        // Combine the results
        result.append(intBinary);
        result.append(".");
        result.append(fracBinary.length() > 0 ? fracBinary.toString() : "0");
        return (isNegative ? "-" : "") + result.toString();
    }
    // Convert a binary string with decimal point to a double
    private static double binaryStringToDouble(String binary) {
        boolean isNegative = binary.startsWith("-");
        if (isNegative)
            binary = binary.substring(1);
        String[] parts = binary.split("\\.");
        String intPart = parts[0];
        String fracPart = (parts.length > 1) ? parts[1] : "0";
        double result = 0;
        // Convert integer part
        if (!intPart.equals("0")) {
            result += Long.parseLong(intPart, 2);
        }
        // Convert fractional part
        if (!fracPart.equals("0")) {
            double fraction = 0;
            for (int i = 0; i < fracPart.length(); i++) {
                if (fracPart.charAt(i) == '1') {
                    fraction += 1.0 / (1L << (i + 1));
                }
            }
            result += fraction;
        }
        return isNegative ? -result : result;
    }
    // Binary addition for strings with decimal points
    private static String binaryAdd(String a, String b) {
        boolean aIsNegative = a.startsWith("-");
        boolean bIsNegative = b.startsWith("-");
        if (aIsNegative && !bIsNegative) {
            return binarySubtract(b, a.substring(1));
        } else if (!aIsNegative && bIsNegative) {
            return binarySubtract(a, b.substring(1));
        } else if (aIsNegative && bIsNegative) {
            String result = binaryAdd(a.substring(1), b.substring(1));
            return "-" + result;
        }
        StringBuilder result = new StringBuilder();
        int carry = 0;
        // Split into integer and fractional parts
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        // Align the fractional parts
        int maxFractionLength = Math.max(
                partsA.length > 1 ? partsA[1].length() : 0,
                partsB.length > 1 ? partsB[1].length() : 0);

        String fracA = (partsA.length > 1) ? partsA[1] + "0".repeat(maxFractionLength - partsA[1].length())
                : "0".repeat(maxFractionLength);

        String fracB = (partsB.length > 1) ? partsB[1] + "0".repeat(maxFractionLength - partsB[1].length())
                : "0".repeat(maxFractionLength);

        // Add fractional parts from right to left
        StringBuilder fracResult = new StringBuilder();
        for (int i = maxFractionLength - 1; i >= 0; i--) {
            int bitA = (i < fracA.length()) ? fracA.charAt(i) - '0' : 0;
            int bitB = (i < fracB.length()) ? fracB.charAt(i) - '0' : 0;
            int sum = bitA + bitB + carry;
            fracResult.append(sum % 2);
            carry = sum / 2;
        }
        fracResult.reverse();
        // Add integer parts from right to left
        String intA = partsA[0];
        String intB = partsB[0];
        int maxIntLength = Math.max(intA.length(), intB.length());

        StringBuilder intResult = new StringBuilder();
        for (int i = 1; i <= maxIntLength || carry > 0; i++) {
            int bitA = (i <= intA.length()) ? intA.charAt(intA.length() - i) - '0' : 0;
            int bitB = (i <= intB.length()) ? intB.charAt(intB.length() - i) - '0' : 0;
            int sum = bitA + bitB + carry;
            intResult.append(sum % 2);
            carry = sum / 2;
        }

        // Combine the results
        result.append(intResult.reverse().toString());
        if (maxFractionLength > 0) {
            result.append(".");
            result.append(fracResult.toString());
        }

        // Trim leading zeros in integer part and trailing zeros in fractional part
        String finalResult = result.toString().replaceFirst("^0+(?!$)", "");
        if (finalResult.contains(".")) {
            finalResult = finalResult.replaceFirst("0+$", "");
            if (finalResult.endsWith("."))
                finalResult += "0";
        }

        return finalResult;
    }

    // Binary subtraction
    private static String binarySubtract(String a, String b) {
        boolean aIsNegative = a.startsWith("-");
        boolean bIsNegative = b.startsWith("-");

        if (!aIsNegative && bIsNegative) {
            return binaryAdd(a, b.substring(1));
        } else if (aIsNegative && !bIsNegative) {
            String result = binaryAdd(a.substring(1), b);
            return "-" + result;
        } else if (aIsNegative && bIsNegative) {
            return binarySubtract(b.substring(1), a.substring(1));
        }

        // Check if we need to swap (a < b)
        if (compareBinary(a, b) < 0) {
            String result = binarySubtract(b, a);
            return "-" + result;
        }

        // At this point, we know a >= b and both are positive
        StringBuilder result = new StringBuilder();
        int borrow = 0;

        // Split into integer and fractional parts
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");

        // Align the fractional parts
        int maxFractionLength = Math.max(
                partsA.length > 1 ? partsA[1].length() : 0,
                partsB.length > 1 ? partsB[1].length() : 0);

        String fracA = (partsA.length > 1) ? partsA[1] + "0".repeat(maxFractionLength - partsA[1].length())
                : "0".repeat(maxFractionLength);

        String fracB = (partsB.length > 1) ? partsB[1] + "0".repeat(maxFractionLength - partsB[1].length())
                : "0".repeat(maxFractionLength);

        // Subtract fractional parts from right to left
        StringBuilder fracResult = new StringBuilder();
        for (int i = maxFractionLength - 1; i >= 0; i--) {
            int bitA = (i < fracA.length()) ? fracA.charAt(i) - '0' : 0;
            int bitB = (i < fracB.length()) ? fracB.charAt(i) - '0' : 0;
            bitA -= borrow;

            if (bitA < bitB) {
                bitA += 2;
                borrow = 1;
            } else {
                borrow = 0;
            }

            fracResult.append(bitA - bitB);
        }
        fracResult.reverse();

        // Subtract integer parts from right to left
        String intA = partsA[0];
        String intB = partsB[0];
        int maxIntLength = Math.max(intA.length(), intB.length());

        StringBuilder intResult = new StringBuilder();
        for (int i = 1; i <= maxIntLength; i++) {
            int bitA = (i <= intA.length()) ? intA.charAt(intA.length() - i) - '0' : 0;
            int bitB = (i <= intB.length()) ? intB.charAt(intB.length() - i) - '0' : 0;

            bitA -= borrow;
            if (bitA < bitB) {
                bitA += 2;
                borrow = 1;
            } else {
                borrow = 0;
            }

            intResult.append(bitA - bitB);
        }

        // Combine the results
        result.append(intResult.reverse().toString().replaceFirst("^0+(?!$)", ""));
        if (maxFractionLength > 0) {
            result.append(".");
            result.append(fracResult.toString());
        }

        // Trim leading zeros in integer part and trailing zeros in fractional part
        String finalResult = result.toString().replaceFirst("^0+(?!$)", "");
        if (finalResult.isEmpty() || finalResult.startsWith("."))
            finalResult = "0" + finalResult;
        if (finalResult.contains(".")) {
            finalResult = finalResult.replaceFirst("0+$", "");
            if (finalResult.endsWith("."))
                finalResult += "0";
        }

        return finalResult;
    }

    private static String binaryMultiply(String a, String b) {
        boolean isNegative = a.startsWith("-") ^ b.startsWith("-");

        // Remove negative signs for calculation
        if (a.startsWith("-"))
            a = a.substring(1);
        if (b.startsWith("-"))
            b = b.substring(1);

        // Handle special case of multiplication by zero
        if (a.equals("0.0") || b.equals("0.0"))
            return "0.0";

        // Split into integer and fractional parts
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");

        String intA = partsA[0];
        String fracA = partsA.length > 1 ? partsA[1] : "0";
        String intB = partsB[0];
        String fracB = partsB.length > 1 ? partsB[1] : "0";

        // Calculate number of decimal positions to shift
        int decimalShift = fracA.length() + fracB.length();

        // Create operands without decimal point for integer multiplication
        String operandA = intA + fracA;
        String operandB = intB + fracB;

        // Remove leading zeros
        operandA = operandA.replaceFirst("^0+", "");
        operandB = operandB.replaceFirst("^0+", "");

        // Handle empty strings
        if (operandA.isEmpty())
            operandA = "0";
        if (operandB.isEmpty())
            operandB = "0";

        // Perform binary multiplication (long multiplication algorithm)
        String result = "0";
        for (int i = operandB.length() - 1; i >= 0; i--) {
            if (operandB.charAt(i) == '1') {
                // Shift operandA according to position and add
                String shifted = operandA + "0".repeat(operandB.length() - 1 - i);
                result = binaryAdd(result, shifted);
            }
        }

        // Insert the decimal point
        int resultLength = result.length();
        if (decimalShift >= resultLength) {
            result = "0." + "0".repeat(decimalShift - resultLength) + result;
        } else {
            result = result.substring(0, resultLength - decimalShift) + "." +
                    result.substring(resultLength - decimalShift);
        }

        // Remove leading zeros in integer part and trailing zeros in fractional part
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

    // Binary division - proper implementation
    private static String binaryDivide(String a, String b) {
        // Check for division by zero
        if (b.equals("0.0") || b.equals("0") || b.equals("-0.0")) {
            throw new ArithmeticException("Division par zéro");
        }

        // Handle division of zero
        if (a.equals("0.0") || a.equals("0") || a.equals("-0.0")) {
            return "0.0";
        }

        boolean isNegative = a.startsWith("-") ^ b.startsWith("-");

        // Remove negative signs for calculation
        if (a.startsWith("-"))
            a = a.substring(1);
        if (b.startsWith("-"))
            b = b.substring(1);

        // Split into integer and fractional parts
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");

        String intA = partsA[0];
        String fracA = partsA.length > 1 ? partsA[1] : "0";
        String intB = partsB[0];
        String fracB = partsB.length > 1 ? partsB[1] : "0";

        // Normalize numbers by shifting decimal points to create integers
        String operandA = intA + fracA;
        String operandB = intB + fracB;

        // Remove leading and trailing zeros
        operandA = operandA.replaceFirst("^0+", "");
        operandB = operandB.replaceFirst("^0+", "");

        // Handle empty strings
        if (operandA.isEmpty())
            operandA = "0";
        if (operandB.isEmpty())
            operandB = "0";

        // Shift decimal places to account for fractions
        int decimalAdjustment = fracB.length() - fracA.length();

        // Ensure dividend has enough precision
        int precision = 20; // Controls precision of division result
        operandA = operandA + "0".repeat(precision + Math.max(0, decimalAdjustment));

        // Long division algorithm
        StringBuilder quotient = new StringBuilder();
        StringBuilder remainder = new StringBuilder();

        // Process each digit of operandA
        for (int i = 0; i < operandA.length(); i++) {
            remainder.append(operandA.charAt(i));

            // Remove leading zeros for valid comparison
            String remainderStr = remainder.toString().replaceFirst("^0+", "");
            if (remainderStr.isEmpty())
                remainderStr = "0";

            // Check if we can subtract operandB from current remainder
            if (binaryCompareAbs(remainderStr, operandB) >= 0) {
                quotient.append("1");
                remainder = new StringBuilder(binarySubtractAbs(remainderStr, operandB));
            } else {
                quotient.append("0");
            }
        }

        // Adjust quotient to account for decimal point
        String quotientStr = quotient.toString().replaceFirst("^0+", "");
        if (quotientStr.isEmpty())
            quotientStr = "0";

        // Calculate position for decimal point
        int decimalPos = quotientStr.length() - precision - decimalAdjustment;

        // Insert decimal point
        String result;
        if (decimalPos <= 0) {
            result = "0." + "0".repeat(-decimalPos) + quotientStr;
        } else if (decimalPos >= quotientStr.length()) {
            result = quotientStr + "0".repeat(decimalPos - quotientStr.length()) + ".0";
        } else {
            result = quotientStr.substring(0, decimalPos) + "." + quotientStr.substring(decimalPos);
        }

        // Remove trailing zeros in fractional part
        if (result.contains(".")) {
            result = result.replaceFirst("0+$", "");
            if (result.endsWith("."))
                result += "0";
        }

        return isNegative ? "-" + result : result;
    }

    // Helper method for binary division - absolute value comparison
    private static int binaryCompareAbs(String a, String b) {
        // Remove any negative signs for comparison
        if (a.startsWith("-"))
            a = a.substring(1);
        if (b.startsWith("-"))
            b = b.substring(1);

        // Compare lengths first
        if (a.length() != b.length()) {
            return a.length() > b.length() ? 1 : -1;
        }

        // Compare digits
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.charAt(i) > b.charAt(i) ? 1 : -1;
            }
        }

        // Equal
        return 0;
    }

    // Helper method for binary division - absolute value subtraction
    private static String binarySubtractAbs(String a, String b) {
        // Remove any negative signs
        if (a.startsWith("-"))
            a = a.substring(1);
        if (b.startsWith("-"))
            b = b.substring(1);

        // Ensure a >= b
        if (binaryCompareAbs(a, b) < 0) {
            String temp = a;
            a = b;
            b = temp;
        }

        // Pad b with leading zeros to match length of a
        b = "0".repeat(a.length() - b.length()) + b;

        StringBuilder result = new StringBuilder();
        int borrow = 0;

        // Subtract from right to left
        for (int i = a.length() - 1; i >= 0; i--) {
            int digitA = a.charAt(i) - '0';
            int digitB = b.charAt(i) - '0';

            digitA -= borrow;
            if (digitA < digitB) {
                digitA += 2;
                borrow = 1;
            } else {
                borrow = 0;
            }

            result.append(digitA - digitB);
        }

        // Reverse the result
        return result.reverse().toString().replaceFirst("^0+", "");
    }

    // Compare two binary strings (ignoring decimal points)
    private static int compareBinary(String a, String b) {
        // Remove any negative signs for comparison
        boolean aIsNegative = a.startsWith("-");
        boolean bIsNegative = b.startsWith("-");

        if (aIsNegative && !bIsNegative)
            return -1;
        if (!aIsNegative && bIsNegative)
            return 1;

        // Both have same sign, remove negative signs if present
        if (aIsNegative)
            a = a.substring(1);
        if (bIsNegative)
            b = b.substring(1);

        // Split into integer and fractional parts
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");

        // Compare integer parts
        String intA = partsA[0].replaceFirst("^0+", "");
        if (intA.isEmpty())
            intA = "0";
        String intB = partsB[0].replaceFirst("^0+", "");
        if (intB.isEmpty())
            intB = "0";

        if (intA.length() != intB.length())
            return (intA.length() > intB.length()) ? 1 : -1;

        for (int i = 0; i < intA.length(); i++) {
            if (intA.charAt(i) != intB.charAt(i))
                return (intA.charAt(i) > intB.charAt(i)) ? 1 : -1;
        }

        // Integer parts are equal, compare fractional parts
        String fracA = (partsA.length > 1) ? partsA[1] : "";
        String fracB = (partsB.length > 1) ? partsB[1] : "";

        // Pad with zeros to make comparison easier
        int maxLen = Math.max(fracA.length(), fracB.length());
        fracA = fracA + "0".repeat(maxLen - fracA.length());
        fracB = fracB + "0".repeat(maxLen - fracB.length());

        for (int i = 0; i < maxLen; i++) {
            if (fracA.charAt(i) != fracB.charAt(i))
                return (fracA.charAt(i) > fracB.charAt(i)) ? 1 : -1;
        }

        // They are exactly equal
        return 0;
    }
}