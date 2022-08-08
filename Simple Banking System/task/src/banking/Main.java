package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Scanner;

public class Main {
    Card card = new Card();
    Scanner sc = new Scanner(System.in);
    SQLiteDataSource sds;

    public static void main(String[] args) {
        new Main().go(args);
    }

    void go(String[] args){
        String url = "jdbc:sqlite:".concat(args[1]);
        sds = new SQLiteDataSource();
        sds.setUrl(url);

        try (Connection cn = sds.getConnection()) {
            try (Statement st = cn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS card (id INTEGER PRIMARY KEY, number TEXT NOT NULL, pin TEXT NOT NULL, balance INTEGER DEFAULT 0)");

            } catch (Exception e) {e.printStackTrace();}
        } catch (Exception e) {e.printStackTrace();}

        welcomeText();
    }

    private void welcomeText() {
        System.out.println("1. Create an account");
        System.out.println("2. Log into account");
        System.out.println("0. Exit");

        chooseAction(sc.next());
    }

    private void chooseAction(String input) {
        switch (input){
            case "1": createAccount(); break;
            case "2": logInAccount(); break;
            case "0": exit();
        }
    }

    private void createAccount() {
        Card cd = new Card();
        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(cd.getCardNumber());
        System.out.println("Your card PIN:");
        System.out.println(cd.getPin());

        toDB(cd.getCardNumber(), cd.getPin());
        welcomeText();
    }

    private void toDB(String cardNumber, String pin) {

        try(Connection cn = sds.getConnection()) {
            try (Statement st = cn.createStatement()) {
                st.executeUpdate("INSERT INTO card (number, pin) VALUES ("+ "'" +cardNumber+ "', '" +pin+ "')");
            } catch (Exception e) {e.printStackTrace();}
        } catch (Exception e) {e.printStackTrace();}
    }

    private void logInAccount() {
        System.out.println("Enter your card number:");
        String probeNumber  = sc.next();
        System.out.println("Enter your PIN:");
        String probePin  = sc.next();

        authentication(probeNumber,probePin);
    }
    private void authentication(String probeNumber, String probePin) {

        try (Connection cn = sds.getConnection()) {
            String getBalance = "SELECT balance FROM card WHERE number = ? AND pin = ?";
            try (PreparedStatement st = cn.prepareStatement(getBalance)) {
                st.setString(1, probeNumber);
                st.setString(2, probePin);
                try (ResultSet currentUser = st.executeQuery()) {
                    if (currentUser.next()) {
                        long balance = currentUser.getInt("balance");
                        System.out.println("You have successfully logged in!");
                        insideAccount(balance, probeNumber, probePin, cn);
                    }
                    else {
                        System.out.println("Wrong card number or PIN!");
                        welcomeText();
                    }
                } catch (Exception e) {e.printStackTrace();}
            } catch (Exception e) {e.printStackTrace();}
        } catch (Exception e) {e.printStackTrace();}
    }
    private void insideAccount (long balance, String probeNumber, String probePin, Connection connection) {
        System.out.println("1. Balance");
        System.out.println("2. Add income");
        System.out.println("3. Do transfer");
        System.out.println("4. Close account");
        System.out.println("5. Log out");
        System.out.println("0. Exit");

        switch (sc.next()) {
            case "1": {
                System.out.println("Balance: " + balance);
                insideAccount(balance, probeNumber, probePin, connection);
                break;
            }
            case "2": {
                System.out.println("Enter income:");
                long deposit = sc.nextLong();
                addIncome(balance, probeNumber, probePin, deposit, connection);
                break;
            }
            case "3": {
                System.out.println("Enter card number: ");
                long receiverCard = sc.nextLong();
                makeTransfer(receiverCard, balance, probeNumber, probePin, connection);
                insideAccount(balance, probeNumber, probePin, connection);
                break;
            }
            case "4": {
                deleteAccount(probeNumber, connection);
                welcomeText();
                break;
            }
            case "5": System.out.println("You have successfully logged out!"); welcomeText(); break;
            case "0": exit();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + sc.next());
        }
    }

    private void addIncome(long balance, String probeNumber, String probePin, long deposit, Connection connection) {
        String updateBalance = "UPDATE card SET balance = ? WHERE number = ? AND pin = ?";
        try (PreparedStatement st = connection.prepareStatement(updateBalance)) {
            st.setLong(1, balance += deposit);
            st.setString(2, probeNumber);
            st.setString(3, probePin);
            st.executeUpdate();
            System.out.println("Income was added");
        } catch (Exception e) {e.printStackTrace();}
        insideAccount(balance, probeNumber, probePin, connection);
    }

    private void deleteAccount(String probeNumber, Connection connection) {
        String deleteAccount = "DELETE FROM card WHERE number = ?";
        try (PreparedStatement st = connection.prepareStatement(deleteAccount)) {
            st.setString(1, probeNumber);
            st.executeUpdate();
            System.out.println("The account has been closed!");
        } catch (Exception e) {e.printStackTrace();}
    }

    private void makeTransfer(long receiverCard, long balance, String probeNumber, String probePin, Connection connection){
        long lastDigit = receiverCard % 10;
        long fifteenDigits = receiverCard / 10;
        long expectedResult = Long.parseLong(card.luhnAlgorithm(String.valueOf(fifteenDigits)));
        if (lastDigit == expectedResult){
            String getCardNumber = "SELECT number FROM card WHERE number = ?";
            try (PreparedStatement st = connection.prepareStatement(getCardNumber)) {
                st.setString(1, String.valueOf(receiverCard));
                try (ResultSet user = st.executeQuery()) {
                    if (user.next()) {
                        System.out.println("Enter how much money you want to transfer: ");
                        long transfer = sc.nextLong();
                        if (transfer <= balance){
                            String subtractFromSender = "UPDATE card SET balance = balance - ? WHERE number = ?";
                            String addToReceiver = "UPDATE card SET balance = balance + ? WHERE number = ?";
                            connection.setAutoCommit(false);
                            try (PreparedStatement subtract = connection.prepareStatement(subtractFromSender);
                                 PreparedStatement add = connection.prepareStatement(addToReceiver)) {

                                subtract.setLong(1, transfer);
                                subtract.setString(2, probeNumber);
                                subtract.executeUpdate();

                                add.setLong(1, transfer);
                                add.setLong(2, receiverCard);
                                add.executeUpdate();

                                connection.commit();
                            } catch (SQLException e) {
                                try {
                                    System.err.print("Transaction is being rolled back");
                                    connection.rollback();
                                } catch (SQLException except) {
                                    except.printStackTrace();
                                }
                            }
                            connection.setAutoCommit(true);
                        }
                        else {
                            System.out.println("Not enough money!");
                            insideAccount(balance, probeNumber, probePin, connection);
                        }
                    }
                    else {
                        System.out.println("Such a card does not exist.");
                        insideAccount(balance, probeNumber, probePin, connection);
                    }
                } catch (Exception e) {e.printStackTrace();}
            } catch (Exception e) {e.printStackTrace();}
        }
        else {
            System.out.println("Probably you made a mistake in the card number. Please try again!");
            insideAccount(balance, probeNumber, probePin, connection);
        }
    }

    private void exit() {
        System.out.println("Bye!");
        System.exit(0);
    }
}