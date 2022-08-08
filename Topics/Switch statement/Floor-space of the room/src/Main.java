import java.util.Scanner;


class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // start coding here
        String shape = scanner.nextLine();
        double a;
        double b;
        double c;
        double r;
        switch (shape) {
            case "triangle":
                a = scanner.nextDouble();
                b = scanner.nextDouble();
                c = scanner.nextDouble();
                double p = (a + b + c) / 2;
                double areaT = Math.sqrt(p * (p - a) * (p - b) * (p - c));
                System.out.println(areaT);
                break;
            case "rectangle":
                a = scanner.nextDouble();
                b = scanner.nextDouble();
                double areaR = a * b;
                System.out.println(areaR);
                break;
            case "circle":
                r = scanner.nextDouble();
                double pi = 3.14;
                double areaC = pi * Math.pow(r, 2);
                System.out.println(areaC);
                break;
            default:
                System.out.println("Not a shape!");
                break;
        }
    }
}
