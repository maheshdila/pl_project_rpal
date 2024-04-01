import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        try {
            Scanner scanner = new Scanner("C:\\Users\\Asus\\IdeaProjects\\pl_project\\src\\test\\test01.txt"); // Change "input.txt" to your input file name
            while (scanner.hasMoreTokens()) {
                Token token = scanner.readNextToken();
                if (token != null) {
                    System.out.println(token); // Assuming Token class overrides toString() method
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
        }
    }
}
