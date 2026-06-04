package pt.isel.gape.dev;

/**
 * Aplicacao DEV para testes manuais CRUD da base de dados.
 *
 * Nao usar em producao.
 * Nao expor via web.
 * Nao integrar com login/fluxos funcionais do GAPE.
 */
public final class DevDatabaseCrudConsoleApp {

    private final DevConsoleInputReader input = new DevConsoleInputReader();
    private final DevCrudService crudService = new DevCrudService();

    public static void main(String[] args) {
        new DevDatabaseCrudConsoleApp().run();
    }

    private void run() {
        boolean running = true;

        while (running) {
            printMainMenu();
            int option = input.readInt("Escolha uma opcao: ", 0, 4);
            System.out.println();

            switch (option) {
                case 1 -> crudService.viewData(input);
                case 2 -> crudService.updateData(input);
                case 3 -> crudService.createData(input);
                case 4 -> crudService.deleteData(input);
                case 0 -> running = false;
                default -> System.out.println("Opcao invalida.");
            }

            System.out.println();
        }

        System.out.println("Aplicacao encerrada.");
    }

    private void printMainMenu() {
        System.out.println("==============================");
        System.out.println(" GAPE - CRUD da Base de Dados");
        System.out.println("==============================");
        System.out.println("1. Ver Dados");
        System.out.println("2. Atualizar Dados");
        System.out.println("3. Criar Dados");
        System.out.println("4. Apagar Dados");
        System.out.println("0. Sair");
        System.out.println();
    }
}
