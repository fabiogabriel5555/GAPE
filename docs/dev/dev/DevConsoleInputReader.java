package pt.isel.gape.dev;

import java.util.Scanner;

final class DevConsoleInputReader {

    private final Scanner scanner = new Scanner(System.in);

    String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    int readInt(String prompt, int min, int max) {
        while (true) {
            String raw = readLine(prompt);
            try {
                int value = Integer.parseInt(raw);
                if (value < min || value > max) {
                    System.out.printf("Valor invalido. Introduz um numero entre %d e %d.%n", min, max);
                    continue;
                }
                return value;
            } catch (NumberFormatException ignored) {
                System.out.println("Valor invalido. Introduz um numero.");
            }
        }
    }

    boolean confirm(String prompt) {
        while (true) {
            String raw = readLine(prompt + " [s/n]: ");
            if ("s".equalsIgnoreCase(raw) || "sim".equalsIgnoreCase(raw)) {
                return true;
            }
            if ("n".equalsIgnoreCase(raw) || "nao".equalsIgnoreCase(raw)) {
                return false;
            }
            System.out.println("Resposta invalida. Usa 's' ou 'n'.");
        }
    }
}
