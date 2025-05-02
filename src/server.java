import java.io.*;
import java.net.*;
import java.util.Stack;
public class server {
    public static int port = 1234;
    private static DatagramSocket socket; 
    public static void main(String[] args) throws Exception {
        socket = new DatagramSocket(port);
        System.out.println("Server is running on port " + port);
        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String messageWithChecksum = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received: " + messageWithChecksum);
            // Split message and checksum
            String[] parts = messageWithChecksum.split("\\|");
            if (parts.length != 2) {
                sendToClient("Erreur: Format invalide", packet.getAddress(), packet.getPort());
                continue;
            }
            String message = parts[0];
            int receivedChecksum;
            try {
                receivedChecksum = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                sendToClient("Erreur: Checksum invalide", packet.getAddress(), packet.getPort());
                continue;
            }
            // Calculate checksum on received message
            int calculatedChecksum = calculateChecksum(message);
            // Compare checksums
            if (calculatedChecksum != receivedChecksum) {
                System.out.println("Checksum error: expected " + receivedChecksum + 
                                  ", calculated " + calculatedChecksum);
                sendToClient("Erreur: Données corrompues", packet.getAddress(), packet.getPort());
            } else {
                // Process as usual
                try {
                    double result = calculate(message);
                    String resultStr = String.valueOf(result);
                    sendToClient(resultStr, packet.getAddress(), packet.getPort());
                } catch (Exception e) {
                    sendToClient("Erreur: " + e.getMessage(), packet.getAddress(), packet.getPort());
                }
            }
        }
    }

   /*  private static int calculateChecksum(String message) {
        // Same method as in client
        int sum = 0;
        for (char c : message.toCharArray()) {
            sum += c;
        }
        return sum;
    } */
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



























    private static void sendToClient(String message, InetAddress address, int port) throws IOException {
        byte[] buf = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        
        // Try sending multiple times to increase reliability
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                socket.send(packet);
                System.out.println("Sent to client (" + address + ":" + port + "): " + message);
                System.out.println("Message bytes sent: " + buf.length);
                // Successfully sent, no need for further attempts
                break;
            } catch (IOException e) {
                System.out.println("Send attempt " + (attempt + 1) + " failed: " + e.getMessage());
                if (attempt == 2) {
                    // Rethrow on last attempt
                    throw e;
                }
                // Wait a bit before retrying
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    //shuning yard
    public static double calculate(String expression) {
        expression = expression.replace("÷", "/").replace("x", "*").replaceAll(" ", "");
        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();
        int i = 0;
        while (i < expression.length()) {
            char ch = expression.charAt(i);
            if (ch == ' ') {
                i++;
                continue;
            }
            if (Character.isDigit(ch) || ch == '.' || (ch == '-' &&
                (i == 0 || !Character.isDigit(expression.charAt(i - 1)) && expression.charAt(i - 1) != ')'))) {
                StringBuilder sb = new StringBuilder();
                if (ch == '-') {
                    sb.append('-');
                    i++;
                }
                while (i < expression.length() &&
                      (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    sb.append(expression.charAt(i));
                    i++;
                }
                numbers.push(Double.parseDouble(sb.toString()));
                if (i < expression.length() && expression.charAt(i) == '%') {
                    double value = numbers.pop() / 100.0;
                    numbers.push(value);
                    i++;
                }
            }
            else if (ch == '+' || ch == '-' || ch == '*' || ch == '/') {
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(ch)) {
                    double b = numbers.pop();
                    double a = numbers.pop();
                    numbers.push(applyOp(a, b, operators.pop()));
                }
                operators.push(ch);
                i++;
            }
            else if (ch == '%') {
                i++;
            }
            else {
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
            case '-': return 1;
            case '*':
            case '/': return 2;
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
                if (b == 0) throw new ArithmeticException("Division par zéro");
                resultBinary = binaryDivide(binaryA, binaryB);
                break;
            default:
                throw new UnsupportedOperationException("Opérateur non supporté: " + op);
        }
        
        return binaryStringToDouble(resultBinary);
    } 
    // Convert a double to a binary string with decimal point//khtr adition ta azouza tlwj taaml kisma fi wist bi . w ken dima bi norme iee moch mnjm yalkaha 
    /*The error you were seeing "Index 1 out of bounds for length 1" was
     because your friend's code expected strings with decimal points,
      but your code was passing IEEE 754 binary representations without decimal points. */
    private static String doubleToBinaryString(double num) {
        if (num == 0) return "0.0";
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
        for (int i = 0; i < 10 && fracPart > 0; i++) {
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
        if (isNegative) binary = binary.substring(1);
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
            partsB.length > 1 ? partsB[1].length() : 0
        );
        
        String fracA = (partsA.length > 1) ? 
            partsA[1] + "0".repeat(maxFractionLength - partsA[1].length()) : 
            "0".repeat(maxFractionLength);
            
        String fracB = (partsB.length > 1) ? 
            partsB[1] + "0".repeat(maxFractionLength - partsB[1].length()) : 
            "0".repeat(maxFractionLength);
    
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
            if (finalResult.endsWith(".")) finalResult += "0";
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
            partsB.length > 1 ? partsB[1].length() : 0
        );
        
        String fracA = (partsA.length > 1) ? 
            partsA[1] + "0".repeat(maxFractionLength - partsA[1].length()) : 
            "0".repeat(maxFractionLength);
            
        String fracB = (partsB.length > 1) ? 
            partsB[1] + "0".repeat(maxFractionLength - partsB[1].length()) : 
            "0".repeat(maxFractionLength);
        
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
        if (finalResult.isEmpty() || finalResult.startsWith(".")) finalResult = "0" + finalResult;
        if (finalResult.contains(".")) {
            finalResult = finalResult.replaceFirst("0+$", "");
            if (finalResult.endsWith(".")) finalResult += "0";
        }
        
        return finalResult;
    }
    
    private static String binaryMultiply(String a, String b) {
        boolean isNegative = a.startsWith("-") ^ b.startsWith("-");
        
        // Remove negative signs for calculation
        if (a.startsWith("-")) a = a.substring(1);
        if (b.startsWith("-")) b = b.substring(1);
        
        // Handle special case of multiplication by zero
        if (a.equals("0.0") || b.equals("0.0")) return "0.0";
        
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
        if (operandA.isEmpty()) operandA = "0";
        if (operandB.isEmpty()) operandB = "0";
        
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
        if (finalResult.startsWith(".")) finalResult = "0" + finalResult;
        if (finalResult.contains(".")) {
            finalResult = finalResult.replaceFirst("0+$", "");
            if (finalResult.endsWith(".")) finalResult += "0";
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
        if (a.startsWith("-")) a = a.substring(1);
        if (b.startsWith("-")) b = b.substring(1);
        
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
        if (operandA.isEmpty()) operandA = "0";
        if (operandB.isEmpty()) operandB = "0";
        
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
            if (remainderStr.isEmpty()) remainderStr = "0";
            
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
        if (quotientStr.isEmpty()) quotientStr = "0";
        
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
            if (result.endsWith(".")) result += "0";
        }
        
        return isNegative ? "-" + result : result;
    }
    
    // Helper method for binary division - absolute value comparison
    private static int binaryCompareAbs(String a, String b) {
        // Remove any negative signs for comparison
        if (a.startsWith("-")) a = a.substring(1);
        if (b.startsWith("-")) b = b.substring(1);
        
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
        if (a.startsWith("-")) a = a.substring(1);
        if (b.startsWith("-")) b = b.substring(1);
        
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
        
        if (aIsNegative && !bIsNegative) return -1;
        if (!aIsNegative && bIsNegative) return 1;
        
        // Both have same sign, remove negative signs if present
        if (aIsNegative) a = a.substring(1);
        if (bIsNegative) b = b.substring(1);
        
        // Split into integer and fractional parts
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        
        // Compare integer parts
        String intA = partsA[0].replaceFirst("^0+", "");
        if (intA.isEmpty()) intA = "0";
        String intB = partsB[0].replaceFirst("^0+", "");
        if (intB.isEmpty()) intB = "0";
        
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