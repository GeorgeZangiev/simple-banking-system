import java.util.Scanner;

class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        final int b = 10;
        int a = scanner.nextInt();
        boolean result = a < b;
        System.out.println(result);
    }
}